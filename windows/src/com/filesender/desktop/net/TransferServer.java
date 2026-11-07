package com.filesender.desktop.net;

import com.filesender.desktop.storage.ReceiveStore;
import com.filesender.desktop.storage.ReceiveTarget;
import com.filesender.desktop.util.AppConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public final class TransferServer {

    private static final int READ_TIMEOUT_MS = 120000;
    private static final long PROGRESS_INTERVAL_MS = 120L;

    public interface Listener {
        void onReady();

        void onSessionStarted(int fileCount, long totalKnownBytes);

        void onFileStarted(int index, String name, long size);

        void onProgress(int index, String name, long fileBytes, long fileSize,
                        long totalReceived, long totalKnownBytes);

        void onFileSaved(int index, String name, String location);

        void onComplete(int fileCount, long totalReceived);

        void onError(String message);
    }

    private final ReceiveStore store;
    private final Listener listener;

    private volatile boolean running;
    private volatile ServerSocket serverSocket;
    private volatile Socket activeSocket;

    public TransferServer(ReceiveStore store, Listener listener) {
        this.store = store;
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread thread = new Thread(this::bindAndAccept, "FileSender-TransferServer");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        ServerSocket local = serverSocket;
        serverSocket = null;
        if (local != null) {
            try {
                local.close();
            } catch (IOException ignored) {
                // Nothing to do.
            }
        }
        Socket current = activeSocket;
        activeSocket = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
                // Nothing to do.
            }
        }
    }

    private void bindAndAccept() {
        ServerSocket local = null;
        try {
            local = new ServerSocket();
            local.setReuseAddress(true);
            local.bind(new InetSocketAddress(AppConfig.TCP_PORT));
            serverSocket = local;
            if (!running) {
                return;
            }
            if (listener != null) {
                listener.onReady();
            }
            while (running) {
                Socket socket = local.accept();
                activeSocket = socket;
                try {
                    handle(socket);
                } catch (Exception e) {
                    if (running) {
                        notifyError(humanMessage(e));
                    }
                } finally {
                    activeSocket = null;
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                        // Nothing to do.
                    }
                }
            }
        } catch (BindException e) {
            if (running) {
                notifyError("No se pudo abrir el puerto " + AppConfig.TCP_PORT
                        + ". Cierra otra instancia de FileSender.");
            }
        } catch (Exception e) {
            if (running) {
                notifyError("No se pudo iniciar el receptor TCP.");
            }
        } finally {
            running = false;
            if (serverSocket == local) {
                serverSocket = null;
            }
            if (local != null) {
                try {
                    local.close();
                } catch (IOException ignored) {
                    // Nothing to do.
                }
            }
        }
    }

    private void handle(Socket socket) throws IOException {
        socket.setSoTimeout(READ_TIMEOUT_MS);
        socket.setTcpNoDelay(true);
        DataInputStream input = new DataInputStream(
                new BufferedInputStream(socket.getInputStream(), 16 * 1024));
        DataOutputStream output = new DataOutputStream(
                new BufferedOutputStream(socket.getOutputStream(), 4096));

        int magic = input.readInt();
        int version = input.readInt();
        if (magic != TransferProtocol.MAGIC) {
            throw new IOException("El dispositivo emisor no es compatible con esta versión.");
        }
        if (version != TransferProtocol.VERSION) {
            throw new IOException("Versión de transferencia no compatible.");
        }
        int count = input.readInt();
        if (count <= 0 || count > TransferProtocol.MAX_FILES) {
            throw new IOException("Cantidad de archivos no válida.");
        }

        List<IncomingFile> files = new ArrayList<>(count);
        long totalKnown = 0L;
        for (int i = 0; i < count; i++) {
            String name = input.readUTF();
            long size = input.readLong();
            String mime = input.readUTF();
            if (name == null || name.trim().isEmpty()
                    || name.length() > TransferProtocol.MAX_FILE_NAME) {
                throw new IOException("Nombre de archivo no válido.");
            }
            if (size < -1) {
                throw new IOException("Tamaño de archivo no válido.");
            }
            if (mime == null || mime.length() > TransferProtocol.MAX_MIME_LENGTH) {
                mime = "application/octet-stream";
            }
            files.add(new IncomingFile(name, size, mime));
            if (size > 0) {
                totalKnown += size;
            }
        }

        if (listener != null) {
            listener.onSessionStarted(count, totalKnown);
        }

        long totalReceived = 0L;
        for (int i = 0; i < files.size(); i++) {
            if (!running) {
                return;
            }
            IncomingFile file = files.get(i);
            if (listener != null) {
                listener.onFileStarted(i + 1, file.name, file.size);
            }
            ReceiveTarget target = store.create(file.name, file.mime);
            long fileReceived;
            boolean saved = false;
            try {
                try (OutputStream stream = target.open()) {
                    fileReceived = copyFile(input, stream, i + 1, file, totalReceived, totalKnown);
                    stream.flush();
                }
                target.commit();
                saved = true;
            } finally {
                if (!saved) {
                    target.abort();
                }
            }
            totalReceived += fileReceived;
            if (listener != null) {
                listener.onProgress(i + 1, target.getDisplayName(), fileReceived, file.size,
                        totalReceived, totalKnown);
                listener.onFileSaved(i + 1, target.getDisplayName(), target.getLocationLabel());
            }
        }

        output.writeInt(TransferProtocol.ACK_OK);
        output.flush();
        if (listener != null) {
            listener.onComplete(files.size(), totalReceived);
        }
    }

    private long copyFile(DataInputStream input, OutputStream output, int index,
                          IncomingFile file, long previousTotal, long totalKnown) throws IOException {
        byte[] buffer = new byte[TransferProtocol.BUFFER_SIZE];
        long fileReceived = 0L;
        long lastProgress = 0L;

        if (file.size >= 0) {
            long remaining = file.size;
            while (remaining > 0) {
                if (!running) {
                    throw new IOException("Recepción detenida.");
                }
                int wanted = (int) Math.min(buffer.length, remaining);
                int read = input.read(buffer, 0, wanted);
                if (read < 0) {
                    throw new EOFException("La otra tablet cerró la conexión antes de terminar.");
                }
                output.write(buffer, 0, read);
                remaining -= read;
                fileReceived += read;
                long now = System.nanoTime();
                if (now - lastProgress >= PROGRESS_INTERVAL_MS * 1_000_000L) {
                    lastProgress = now;
                    if (listener != null) {
                        listener.onProgress(index, file.name, fileReceived, file.size,
                                previousTotal + fileReceived, totalKnown);
                    }
                }
            }
        } else {
            while (true) {
                if (!running) {
                    throw new IOException("Recepción detenida.");
                }
                int length = input.readInt();
                if (length == 0) {
                    break;
                }
                if (length < 0 || length > TransferProtocol.BUFFER_SIZE) {
                    throw new IOException("Bloque de datos no válido.");
                }
                input.readFully(buffer, 0, length);
                output.write(buffer, 0, length);
                fileReceived += length;
                long now = System.nanoTime();
                if (now - lastProgress >= PROGRESS_INTERVAL_MS * 1_000_000L) {
                    lastProgress = now;
                    if (listener != null) {
                        listener.onProgress(index, file.name, fileReceived, file.size,
                                previousTotal + fileReceived, totalKnown);
                    }
                }
            }
        }

        if (listener != null) {
            listener.onProgress(index, file.name, fileReceived, file.size,
                    previousTotal + fileReceived, totalKnown);
        }
        return fileReceived;
    }

    private void notifyError(String message) {
        if (listener != null) {
            listener.onError(message);
        }
    }

    private static String humanMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Se interrumpió la transferencia.";
        }
        if (message.startsWith("La otra tablet")
                || message.startsWith("No se pudo")
                || message.startsWith("El dispositivo")
                || message.startsWith("Cantidad")
                || message.startsWith("Nombre")
                || message.startsWith("Tamaño")
                || message.startsWith("Versión")
                || message.startsWith("Bloque")
                || message.startsWith("Recepción")) {
            return message;
        }
        return "No se pudo recibir: " + message;
    }

    private static final class IncomingFile {
        final String name;
        final long size;
        final String mime;

        IncomingFile(String name, long size, String mime) {
            this.name = name;
            this.size = size;
            this.mime = mime;
        }
    }
}
