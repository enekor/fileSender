package com.filesender.desktop.net;

import com.filesender.desktop.SelectedFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

public final class TransferClient {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 90000;
    private static final long PROGRESS_INTERVAL_MS = 120L;

    public interface Listener {
        void onStarted(int fileCount, long totalKnownBytes);

        void onFileStarted(int index, String name, long size, long totalSent, long totalKnownBytes);

        void onProgress(int index, String name, long fileBytes, long fileSize,
                        long totalSent, long totalKnownBytes);

        void onFileSent(int index, String name);

        void onComplete(int fileCount, long totalSent);

        void onError(String message);
    }

    private final DeviceInfo device;
    private final List<SelectedFile> files;
    private final Listener listener;

    private volatile boolean running;
    private volatile boolean cancelled;
    private volatile Socket socket;

    public TransferClient(DeviceInfo device, List<SelectedFile> files, Listener listener) {
        this.device = device;
        this.files = files;
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread thread = new Thread(this::send, "FileSender-TransferClient");
        thread.setDaemon(true);
        thread.start();
    }

    public void cancel() {
        cancelled = true;
        running = false;
        Socket current = socket;
        socket = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
                // Nothing to do.
            }
        }
    }

    private void send() {
        Socket local = null;
        try {
            local = new Socket();
            socket = local;
            local.connect(new InetSocketAddress(device.address, device.port), CONNECT_TIMEOUT_MS);
            local.setSoTimeout(READ_TIMEOUT_MS);
            local.setTcpNoDelay(true);
            try {
                local.setSendBufferSize(256 * 1024);
            } catch (Exception ignored) {
                // The default is fine.
            }

            DataOutputStream output = new DataOutputStream(
                    new BufferedOutputStream(local.getOutputStream(), TransferProtocol.BUFFER_SIZE));
            DataInputStream input = new DataInputStream(
                    new BufferedInputStream(local.getInputStream(), 16 * 1024));

            long totalKnown = 0L;
            for (SelectedFile file : files) {
                if (file.getSize() > 0) {
                    totalKnown += file.getSize();
                }
            }
            if (listener != null) {
                listener.onStarted(files.size(), totalKnown);
            }

            output.writeInt(TransferProtocol.MAGIC);
            output.writeInt(TransferProtocol.VERSION);
            output.writeInt(files.size());
            for (SelectedFile file : files) {
                output.writeUTF(safeName(file.getDisplayName()));
                output.writeLong(file.getSize());
                output.writeUTF(safeMime(file.getMimeType()));
            }
            output.flush();

            long totalSent = 0L;
            for (int i = 0; i < files.size(); i++) {
                if (cancelled) {
                    return;
                }
                SelectedFile file = files.get(i);
                if (listener != null) {
                    listener.onFileStarted(i + 1, file.getDisplayName(), file.getSize(),
                            totalSent, totalKnown);
                }
                try (InputStream stream = file.open()) {
                    byte[] buffer = new byte[TransferProtocol.BUFFER_SIZE];
                    long fileSent = 0L;
                    long lastProgress = 0L;
                    if (file.getSize() >= 0) {
                        long remaining = file.getSize();
                        while (remaining > 0) {
                            if (cancelled) {
                                return;
                            }
                            int wanted = (int) Math.min(buffer.length, remaining);
                            int read = stream.read(buffer, 0, wanted);
                            if (read < 0) {
                                throw new EOFException("El archivo «" + file.getDisplayName()
                                        + "» terminó antes de lo esperado.");
                            }
                            output.write(buffer, 0, read);
                            remaining -= read;
                            fileSent += read;
                            totalSent += read;
                            long now = System.nanoTime();
                            if (now - lastProgress >= PROGRESS_INTERVAL_MS * 1_000_000L) {
                                lastProgress = now;
                                if (listener != null) {
                                    listener.onProgress(i + 1, file.getDisplayName(), fileSent,
                                            file.getSize(), totalSent, totalKnown);
                                }
                            }
                        }
                    } else {
                        int read;
                        while ((read = stream.read(buffer)) >= 0) {
                            if (cancelled) {
                                return;
                            }
                            if (read == 0) {
                                continue;
                            }
                            output.writeInt(read);
                            output.write(buffer, 0, read);
                            fileSent += read;
                            totalSent += read;
                            long now = System.nanoTime();
                            if (now - lastProgress >= PROGRESS_INTERVAL_MS * 1_000_000L) {
                                lastProgress = now;
                                if (listener != null) {
                                    listener.onProgress(i + 1, file.getDisplayName(), fileSent,
                                            file.getSize(), totalSent, totalKnown);
                                }
                            }
                        }
                        output.writeInt(0);
                    }
                    output.flush();
                    if (listener != null) {
                        listener.onProgress(i + 1, file.getDisplayName(), fileSent,
                                file.getSize(), totalSent, totalKnown);
                        listener.onFileSent(i + 1, file.getDisplayName());
                    }
                }
            }

            output.flush();
            int ack = input.readInt();
            if (ack != TransferProtocol.ACK_OK) {
                throw new IOException("El receptor no confirmó la transferencia.");
            }
            if (listener != null) {
                listener.onComplete(files.size(), totalSent);
            }
        } catch (ConnectException e) {
            if (!cancelled) {
                notifyError("No se pudo conectar. Comprueba que el otro dispositivo siga en Recibir.");
            }
        } catch (SocketTimeoutException e) {
            if (!cancelled) {
                notifyError("El otro dispositivo tardó demasiado en responder.");
            }
        } catch (UnknownHostException e) {
            if (!cancelled) {
                notifyError("No se encontró el dispositivo en la red.");
            }
        } catch (Exception e) {
            if (!cancelled) {
                String message = e.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "Se interrumpió la conexión con el otro dispositivo.";
                }
                notifyError(message);
            }
        } finally {
            running = false;
            socket = null;
            if (local != null) {
                try {
                    local.close();
                } catch (IOException ignored) {
                    // Nothing to do.
                }
            }
        }
    }

    private void notifyError(String message) {
        if (listener != null) {
            listener.onError(message);
        }
    }

    private static String safeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "archivo";
        }
        String clean = name.replace('\n', ' ').replace('\r', ' ');
        if (clean.length() > TransferProtocol.MAX_FILE_NAME) {
            clean = clean.substring(0, TransferProtocol.MAX_FILE_NAME);
        }
        return clean;
    }

    private static String safeMime(String mime) {
        if (mime == null) {
            return "application/octet-stream";
        }
        String clean = mime.trim();
        if (clean.isEmpty() || clean.length() > TransferProtocol.MAX_MIME_LENGTH) {
            return "application/octet-stream";
        }
        return clean;
    }
}
