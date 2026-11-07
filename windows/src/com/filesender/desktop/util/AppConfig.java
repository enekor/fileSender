package com.filesender.desktop.util;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

public final class AppConfig {

    public static final String APP_NAME = "FileSender";
    public static final String APP_FOLDER = "FileSender";
    public static final int UDP_PORT = 47821;
    public static final int TCP_PORT = 47822;
    public static final String VERSION = "1.0";

    private static final String ID_FILE = "device-id.txt";

    private AppConfig() {
    }

    public static String deviceName() {
        String name = System.getenv("COMPUTERNAME");
        if (name == null || name.trim().isEmpty()) {
            name = System.getenv("HOSTNAME");
        }
        if (name == null || name.trim().isEmpty()) {
            name = "PC Windows";
        }
        return cleanWireName(name);
    }

    public static synchronized String deviceId() {
        try {
            Path folder = appDataDir();
            Files.createDirectories(folder);
            Path file = folder.resolve(ID_FILE);
            if (Files.isRegularFile(file)) {
                String value = Files.readString(file, StandardCharsets.UTF_8).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
            String id = UUID.randomUUID().toString();
            Files.writeString(file, id, StandardCharsets.UTF_8);
            return id;
        } catch (Exception ignored) {
            return UUID.randomUUID().toString();
        }
    }

    public static Path appDataDir() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.trim().isEmpty()) {
            return Paths.get(appData, APP_FOLDER);
        }
        return Paths.get(System.getProperty("user.home"), "AppData", "Roaming", APP_FOLDER);
    }

    public static Path downloadsDir() {
        String home = System.getProperty("user.home");
        return Paths.get(home, "Downloads", APP_FOLDER);
    }

    public static void ensureDownloadsDir() throws IOException {
        Path folder = downloadsDir();
        Files.createDirectories(folder);
        if (!Files.isDirectory(folder) || !Files.isWritable(folder)) {
            throw new IOException("No se puede escribir en " + folder);
        }
        try (var stream = Files.list(folder)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith(".filesender-") && n.endsWith(".part");
                    })
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // A stale temporary file is harmless.
                        }
                    });
        }
    }

    public static String cleanWireName(String value) {
        if (value == null) {
            return "PC Windows";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length() && builder.length() < 64; i++) {
            char c = value.charAt(i);
            if (c == '|' || c == '\n' || c == '\r' || c == '\t' || Character.isISOControl(c)) {
                builder.append(' ');
            } else {
                builder.append(c);
            }
        }
        String clean = builder.toString().trim();
        return clean.isEmpty() ? "PC Windows" : clean;
    }

    public static String mimeType(Path file) {
        try {
            String type = Files.probeContentType(file);
            if (type != null && !type.isBlank()) {
                return type;
            }
        } catch (Exception ignored) {
            // Fall through to the extension based lookup.
        }
        String guessed = URLConnection.guessContentTypeFromName(file.getFileName().toString());
        return guessed == null ? "application/octet-stream" : guessed;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            bytes = 0;
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0d;
        if (kb < 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", kb);
        }
        double mb = kb / 1024.0d;
        if (mb < 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", mb);
        }
        return String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0d);
    }

    public static String fileCount(int count) {
        return count == 1 ? "1 archivo" : count + " archivos";
    }

    public static int percent(long done, long total) {
        if (total <= 0) {
            return 0;
        }
        long value = Math.round(done * 100.0d / total);
        return (int) Math.max(0, Math.min(100, value));
    }
}
