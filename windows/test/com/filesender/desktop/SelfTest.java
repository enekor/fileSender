package com.filesender.desktop;

import com.filesender.desktop.net.DeviceInfo;
import com.filesender.desktop.net.DiscoveryProtocol;
import com.filesender.desktop.net.DiscoveryResponder;
import com.filesender.desktop.net.TransferClient;
import com.filesender.desktop.net.TransferProtocol;
import com.filesender.desktop.net.TransferServer;
import com.filesender.desktop.storage.DownloadStore;
import com.filesender.desktop.storage.ReceiveStore;
import com.filesender.desktop.storage.ReceiveTarget;
import com.filesender.desktop.util.AppConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class SelfTest {

    private static int checks;

    private SelfTest() {
    }

    public static void main(String[] args) throws Exception {
        testFileNames();
        testDiscoveryProtocol();
        testDiscoveryResponder();
        testTcpTransfers();
        System.out.println("ALL TESTS PASSED (" + checks + " checks)");
    }

    private static void testFileNames() {
        check("evil.txt".equals(DownloadStore.sanitizeFileName("../../evil.txt")), "path traversal");
        check("evil.txt".equals(DownloadStore.sanitizeFileName("..\\..\\evil.txt")), "backslash traversal");
        check("_CON.txt".equals(DownloadStore.sanitizeFileName("CON.txt")), "reserved name");
        check("a_b_c.txt".equals(DownloadStore.sanitizeFileName("a?b*c.txt")), "unsafe chars");
        check("archivo".equals(DownloadStore.sanitizeFileName("...")), "dot name");
        check("foto (1).jpg".equals(DownloadStore.withSuffix("foto.jpg", 1)), "suffix");
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longName.append('a');
        }
        longName.append(".txt");
        String sanitized = DownloadStore.sanitizeFileName(longName.toString());
        check(sanitized.length() <= 180 && sanitized.endsWith(".txt"), "long name");
    }

    private static void testDiscoveryProtocol() {
        byte[] reply = DiscoveryProtocol.reply("id-1", "Tablet|Cocina", 47822);
        DeviceInfo device = DiscoveryProtocol.parseReply(reply, reply.length, "192.168.1.9");
        check(device != null, "discovery reply parses");
        check(device != null && "id-1".equals(device.id), "discovery id");
        check(device != null && "Tablet Cocina".equals(device.name), "discovery name sanitized");
        check(device != null && "192.168.1.9".equals(device.address), "discovery address");
        check(device != null && device.port == 47822, "discovery port");
    }

    private static void testDiscoveryResponder() throws Exception {
        AtomicReference<String> error = new AtomicReference<>();
        DiscoveryResponder responder = new DiscoveryResponder(error::set);
        responder.start();
        try {
            Thread.sleep(400);
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(3000);
                byte[] request = DiscoveryProtocol.request();
                socket.send(new DatagramPacket(request, request.length,
                        InetAddress.getByName("127.0.0.1"), AppConfig.UDP_PORT));
                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                DeviceInfo device = DiscoveryProtocol.parseReply(
                        buffer, packet.getLength(), packet.getAddress().getHostAddress());
                check(device != null, "UDP responder replies");
                check(device != null && AppConfig.TCP_PORT == device.port, "UDP responder port");
                check(error.get() == null, "UDP responder no error: " + error.get());
            }
        } finally {
            responder.stop();
            Thread.sleep(250);
        }
    }

    private static void testTcpTransfers() throws Exception {
        Path root = Files.createTempDirectory("filesender-selftest");
        Path source = Files.createDirectories(root.resolve("source"));
        Path destination = Files.createDirectories(root.resolve("destination"));

        Path text = source.resolve("nota.txt");
        Files.writeString(text, "Hola desde Windows\nAcentos: ñ á é í ó ú\n", StandardCharsets.UTF_8);
        Path empty = source.resolve("vacio.bin");
        Files.write(empty, new byte[0]);
        Path binary = source.resolve("datos.bin");
        byte[] binaryData = new byte[180_000];
        new Random(7L).nextBytes(binaryData);
        Files.write(binary, binaryData);

        List<SelectedFile> selected = new ArrayList<>();
        selected.add(new SelectedFile(text));
        selected.add(new SelectedFile(empty));
        selected.add(new SelectedFile(binary));

        TempStore store = new TempStore(destination);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch serverDone = new CountDownLatch(1);
        CountDownLatch clientDone = new CountDownLatch(1);
        AtomicReference<String> serverError = new AtomicReference<>();
        AtomicReference<String> clientError = new AtomicReference<>();
        AtomicReference<Integer> completedFiles = new AtomicReference<>(0);

        TransferServer server = new TransferServer(store, new TransferServer.Listener() {
            @Override
            public void onReady() {
                ready.countDown();
            }

            @Override
            public void onSessionStarted(int fileCount, long totalKnownBytes) {
            }

            @Override
            public void onFileStarted(int index, String name, long size) {
            }

            @Override
            public void onProgress(int index, String name, long fileBytes, long fileSize,
                                   long totalReceived, long totalKnownBytes) {
            }

            @Override
            public void onFileSaved(int index, String name, String location) {
            }

            @Override
            public void onComplete(int fileCount, long totalReceived) {
                completedFiles.set(fileCount);
                serverDone.countDown();
            }

            @Override
            public void onError(String message) {
                serverError.compareAndSet(null, message);
                serverDone.countDown();
            }
        });
        server.start();
        try {
            check(ready.await(10, TimeUnit.SECONDS), "TCP server ready");
            DeviceInfo loopback = new DeviceInfo("test", "Loopback", "127.0.0.1", AppConfig.TCP_PORT);
            TransferClient client = new TransferClient(loopback, selected,
                    new TransferClient.Listener() {
                        @Override
                        public void onStarted(int fileCount, long totalKnownBytes) {
                        }

                        @Override
                        public void onFileStarted(int index, String name, long size,
                                                  long totalSent, long totalKnownBytes) {
                        }

                        @Override
                        public void onProgress(int index, String name, long fileBytes, long fileSize,
                                               long totalSent, long totalKnownBytes) {
                        }

                        @Override
                        public void onFileSent(int index, String name) {
                        }

                        @Override
                        public void onComplete(int fileCount, long totalSent) {
                            clientDone.countDown();
                        }

                        @Override
                        public void onError(String message) {
                            clientError.compareAndSet(null, message);
                            clientDone.countDown();
                        }
                    });
            client.start();
            check(serverDone.await(20, TimeUnit.SECONDS), "TCP server complete");
            check(clientDone.await(20, TimeUnit.SECONDS), "TCP client complete");
            check(serverError.get() == null, "TCP server error: " + serverError.get());
            check(clientError.get() == null, "TCP client error: " + clientError.get());
            check(completedFiles.get() == 3, "TCP file count");

            check(Files.readString(destination.resolve("nota.txt"), StandardCharsets.UTF_8)
                    .equals(Files.readString(text, StandardCharsets.UTF_8)), "text content");
            check(Files.size(destination.resolve("vacio.bin")) == 0, "empty content");
            check(java.util.Arrays.equals(Files.readAllBytes(destination.resolve("datos.bin")),
                    binaryData), "binary content");

            // A raw frame with an unknown file size exercises the Android chunked path.
            byte[] unknownData = "bloques de tamano desconocido".getBytes(StandardCharsets.UTF_8);
            try (Socket socket = new Socket("127.0.0.1", AppConfig.TCP_PORT)) {
                socket.setSoTimeout(5000);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                out.writeInt(TransferProtocol.MAGIC);
                out.writeInt(TransferProtocol.VERSION);
                out.writeInt(1);
                out.writeUTF("desconocido.dat");
                out.writeLong(-1);
                out.writeUTF("application/octet-stream");
                out.writeInt(unknownData.length);
                out.write(unknownData);
                out.writeInt(0);
                out.flush();
                check(in.readInt() == TransferProtocol.ACK_OK, "chunked ACK");
            }
            Path unknown = destination.resolve("desconocido.dat");
            check(Files.exists(unknown), "chunked file exists");
            check(java.util.Arrays.equals(Files.readAllBytes(unknown), unknownData), "chunked content");
        } finally {
            server.stop();
            deleteTree(root);
        }
    }

    private static void deleteTree(Path root) {
        try (var stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best effort.
                        }
                    });
        } catch (IOException ignored) {
            // Best effort.
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError("FAILED: " + label);
        }
    }

    private static final class TempStore implements ReceiveStore {
        private final Path directory;

        TempStore(Path directory) {
            this.directory = directory;
        }

        @Override
        public ReceiveTarget create(String requestedName, String mimeType) throws IOException {
            String name = DownloadStore.sanitizeFileName(requestedName);
            Path destination = directory.resolve(name);
            int index = 1;
            while (Files.exists(destination)) {
                destination = directory.resolve(DownloadStore.withSuffix(name, index++));
            }
            Path temp = Files.createTempFile(directory, "receive-", ".part");
            Path finalDestination = destination;
            String displayName = finalDestination.getFileName().toString();
            return new ReceiveTarget() {
                @Override
                public OutputStream open() throws IOException {
                    return new FileOutputStream(temp.toFile());
                }

                @Override
                public void commit() throws IOException {
                    try {
                        Files.move(temp, finalDestination);
                    } catch (IOException e) {
                        throw e;
                    }
                }

                @Override
                public void abort() {
                    try {
                        Files.deleteIfExists(temp);
                    } catch (IOException ignored) {
                        // Best effort.
                    }
                }

                @Override
                public String getDisplayName() {
                    return displayName;
                }

                @Override
                public String getLocationLabel() {
                    return finalDestination.toString();
                }
            };
        }
    }
}
