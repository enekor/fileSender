package com.filesender.app;

import com.filesender.app.net.DeviceInfo;
import com.filesender.app.net.TransferClient;
import com.filesender.app.net.TransferServer;
import com.filesender.app.storage.DownloadStore;
import com.filesender.app.storage.ReceiveStore;
import com.filesender.app.storage.ReceiveTarget;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TransferRoundTripTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void transfersSeveralFilesIncludingEmptyAndUnknownSize() throws Exception {
        File source = temporary.newFolder("source");
        File destination = temporary.newFolder("destination");

        File text = new File(source, "nota.txt");
        Files.write(text.toPath(), "Hola desde la cocina\n".getBytes("UTF-8"));
        File empty = new File(source, "vacio.bin");
        Files.write(empty.toPath(), new byte[0]);
        File binary = new File(source, "receta.bin");
        byte[] binaryData = new byte[220_000];
        new Random(7L).nextBytes(binaryData);
        Files.write(binary.toPath(), binaryData);
        File unknown = new File(source, "sin-tamano.dat");
        byte[] unknownData = new byte[31_337];
        new Random(11L).nextBytes(unknownData);
        Files.write(unknown.toPath(), unknownData);

        List<SelectedFile> files = new ArrayList<>();
        files.add(SelectedFile.forLocalFile(text, text.length()));
        files.add(SelectedFile.forLocalFile(empty, 0L));
        files.add(SelectedFile.forLocalFile(binary, binary.length()));
        files.add(SelectedFile.forLocalFile(unknown, -1L));

        final CountDownLatch serverReady = new CountDownLatch(1);
        final CountDownLatch serverDone = new CountDownLatch(1);
        final CountDownLatch clientDone = new CountDownLatch(1);
        final AtomicReference<String> serverError = new AtomicReference<>();
        final AtomicReference<String> clientError = new AtomicReference<>();
        final AtomicReference<Integer> savedCount = new AtomicReference<>(0);
        final AtomicReference<Long> sentBytes = new AtomicReference<>(0L);

        ReceiveStore store = new TempStore(destination);
        TransferServer server = new TransferServer(store, new TransferServer.Listener() {
            @Override
            public void onReady() {
                serverReady.countDown();
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
                savedCount.set(index);
            }

            @Override
            public void onComplete(int fileCount, long totalReceived) {
                serverDone.countDown();
            }

            @Override
            public void onError(String message) {
                serverError.compareAndSet(null, message);
                serverDone.countDown();
            }
        });
        server.start();

        DeviceInfo device = new DeviceInfo("test", "Test", "127.0.0.1", 47822);
        TransferClient client = new TransferClient(null, device, files, new TransferClient.Listener() {
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
                sentBytes.set(totalSent);
                clientDone.countDown();
            }

            @Override
            public void onError(String message) {
                clientError.compareAndSet(null, message);
                clientDone.countDown();
            }
        });

        try {
            assertTrue("server did not start", serverReady.await(10, TimeUnit.SECONDS));
            client.start();
            assertTrue("server timeout", serverDone.await(20, TimeUnit.SECONDS));
            assertTrue("client timeout", clientDone.await(20, TimeUnit.SECONDS));
        } finally {
            client.cancel();
            server.stop();
        }

        assertNull("server: " + serverError.get(), serverError.get());
        assertNull("client: " + clientError.get(), clientError.get());
        assertEquals(Integer.valueOf(4), savedCount.get());
        assertEquals(text.length() + binary.length() + unknownData.length, sentBytes.get().longValue());

        assertArrayEquals(Files.readAllBytes(text.toPath()),
                Files.readAllBytes(new File(destination, "nota.txt").toPath()));
        assertArrayEquals(binaryData,
                Files.readAllBytes(new File(destination, "receta.bin").toPath()));
        assertEquals(0L, new File(destination, "vacio.bin").length());
        assertArrayEquals(unknownData,
                Files.readAllBytes(new File(destination, "sin-tamano.dat").toPath()));
    }

    private static final class TempStore implements ReceiveStore {
        private final File directory;

        TempStore(File directory) {
            this.directory = directory;
        }

        @Override
        public ReceiveTarget create(String requestedName, String mimeType) throws IOException {
            String name = DownloadStore.sanitizeFileName(requestedName);
            File destination = new File(directory, name);
            int index = 1;
            while (destination.exists()) {
                destination = new File(directory, DownloadStore.withSuffix(name, index++));
            }
            File temp = File.createTempFile("receive-", ".part", directory);
            File finalDestination = destination;
            String displayName = finalDestination.getName();
            return new ReceiveTarget() {
                @Override
                public OutputStream open() throws IOException {
                    return new FileOutputStream(temp);
                }

                @Override
                public void commit() throws IOException {
                    if (!temp.renameTo(finalDestination)) {
                        throw new IOException("rename failed");
                    }
                }

                @Override
                public void abort() {
                    //noinspection ResultOfMethodCallIgnored
                    temp.delete();
                }

                @Override
                public String getDisplayName() {
                    return displayName;
                }

                @Override
                public String getLocationLabel() {
                    return finalDestination.getAbsolutePath();
                }
            };
        }
    }
}
