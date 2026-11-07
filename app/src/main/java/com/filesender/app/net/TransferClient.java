package com.filesender.app.net;

import android.content.Context;
import android.util.Log;

import com.filesender.app.SelectedFile;

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

    private static final String TAG = "FileSenderTransfer";
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

    private final Context context;
    private final DeviceInfo device;
    private final List<SelectedFile> files;
    private final Listener listener;

    private volatile boolean running;
    private volatile boolean cancelled;
    private volatile Socket socket;
    private Thread thread;

    public TransferClient(Context context, DeviceInfo device,
                          List<SelectedFile> files, Listener listener) {
        this.context = context == null ? null : context.getApplicationContext();
        this.device = device;
        this.files = files;
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                send();
            }
        }, "FileSender-TransferClient");
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
            } catch (Exception ignored) {
                // Nothing to do.
            }
        }
    }

    private void send() {
        Socket localSocket = null;
        try {
            localSocket = new Socket();
            socket = localSocket;
            localSocket.connect(new InetSocketAddress(device.address, device.port), CONNECT_TIMEOUT_MS);
            localSocket.setSoTimeout(READ_TIMEOUT_MS);
            localSocket.setTcpNoDelay(true);
            try {
                localSocket.setSendBufferSize(256 * 1024);
            } catch (Exception ignored) {
                // The default buffer is still fine.
            }

            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(localSocket.getOutputStream(), TransferProtocol.BUFFER_SIZE));
            DataInputStream in = new DataInputStream(
                    new BufferedInputStream(localSocket.getInputStream(), 16 * 1024));

            long totalKnown = 0L;
            for (SelectedFile file : files) {
                if (file.getSize() > 0) {
                    totalKnown += file.getSize();
                }
            }
            if (listener != null) {
                listener.onStarted(files.size(), totalKnown);
            }

            out.writeInt(TransferProtocol.MAGIC);
            out.writeInt(TransferProtocol.VERSION);
            out.writeInt(files.size());
            for (SelectedFile file : files) {
                out.writeUTF(safeName(file.getDisplayName()));
                out.writeLong(file.getSize());
                out.writeUTF(safeMime(file.getMimeType()));
            }
            out.flush();

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
                InputStream input = null;
                try {
                    input = file.open(context);
                    if (input == null) {
                        throw new IOException("No se pudo leer «" + file.getDisplayName() + "».");
                    }
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
                            int read = input.read(buffer, 0, wanted);
                            if (read < 0) {
                                throw new EOFException("El archivo «" + file.getDisplayName()
                                        + "» terminó antes de lo esperado.");
                            }
                            out.write(buffer, 0, read);
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
                        while ((read = input.read(buffer)) >= 0) {
                            if (cancelled) {
                                return;
                            }
                            if (read == 0) {
                                continue;
                            }
                            out.writeInt(read);
                            out.write(buffer, 0, read);
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
                        out.writeInt(0);
                    }
                    out.flush();
                    if (listener != null) {
                        listener.onProgress(i + 1, file.getDisplayName(), fileSent,
                                file.getSize(), totalSent, totalKnown);
                        listener.onFileSent(i + 1, file.getDisplayName());
                    }
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException ignored) {
                            // Nothing to do.
                        }
                    }
                }
            }

            out.flush();
            int ack = in.readInt();
            if (ack != TransferProtocol.ACK_OK) {
                throw new IOException("El receptor no confirmó la transferencia.");
            }
            if (listener != null) {
                listener.onComplete(files.size(), totalSent);
            }
        } catch (ConnectException e) {
            if (!cancelled) {
                notifyError("No se pudo conectar. Comprueba que la otra tablet siga en Recibir.");
            }
        } catch (SocketTimeoutException e) {
            if (!cancelled) {
                notifyError("La otra tablet tardó demasiado en responder.");
            }
        } catch (UnknownHostException e) {
            if (!cancelled) {
                notifyError("No se encontró el dispositivo en la red.");
            }
        } catch (Exception e) {
            if (!cancelled) {
                Log.w(TAG, "Transfer failed", e);
                String message = e.getMessage();
                if (message == null || message.trim().length() == 0) {
                    message = "Se interrumpió la conexión con la otra tablet.";
                }
                notifyError(message);
            }
        } finally {
            running = false;
            socket = null;
            if (localSocket != null) {
                try {
                    localSocket.close();
                } catch (Exception ignored) {
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
        if (name == null || name.trim().length() == 0) {
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
        if (clean.length() == 0 || clean.length() > TransferProtocol.MAX_MIME_LENGTH) {
            return "application/octet-stream";
        }
        return clean;
    }
}
