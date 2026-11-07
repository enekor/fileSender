package com.filesender.desktop.storage;

import com.filesender.desktop.util.AppConfig;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class DownloadStore implements ReceiveStore {

    private static final int MAX_NAME_LENGTH = 180;
    private static final int IO_BUFFER = 64 * 1024;
    private static final Set<String> RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private final Path folder;

    public DownloadStore() throws IOException {
        this.folder = AppConfig.downloadsDir();
        AppConfig.ensureDownloadsDir();
    }

    public Path getFolder() {
        return folder;
    }

    @Override
    public ReceiveTarget create(String requestedName, String requestedMime) throws IOException {
        AppConfig.ensureDownloadsDir();
        String name = sanitizeFileName(requestedName);
        String uniqueName = uniqueName(folder, name);
        Path destination = folder.resolve(uniqueName);
        Path temp = folder.resolve(".filesender-" + UUID.randomUUID() + ".part");
        Files.createFile(temp);
        return new Target(temp, destination, uniqueName, normalizeMime(requestedMime));
    }

    public static String sanitizeFileName(String requested) {
        String name = requested == null ? "" : requested;
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        StringBuilder clean = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '\0' || Character.isISOControl(c)) {
                continue;
            }
            if (c == '<' || c == '>' || c == ':' || c == '"' || c == '|'
                    || c == '?' || c == '*' || c == '/' || c == '\\') {
                clean.append('_');
            } else {
                clean.append(c);
            }
        }
        name = clean.toString().trim();
        while (name.endsWith(".") || name.endsWith(" ")) {
            name = name.substring(0, name.length() - 1).trim();
        }
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            name = "archivo";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            int dot = name.lastIndexOf('.');
            if (dot > 0 && name.length() - dot <= 12) {
                String extension = name.substring(dot);
                name = name.substring(0, MAX_NAME_LENGTH - extension.length()) + extension;
            } else {
                name = name.substring(0, MAX_NAME_LENGTH);
            }
            name = name.trim();
            while (name.endsWith(".") || name.endsWith(" ")) {
                name = name.substring(0, name.length() - 1);
            }
        }
        String base = name;
        int dot = base.indexOf('.');
        if (dot >= 0) {
            base = base.substring(0, dot);
        }
        if (RESERVED.contains(base.toUpperCase(Locale.ROOT))) {
            name = "_" + name;
        }
        return name.isEmpty() ? "archivo" : name;
    }

    public static String withSuffix(String name, int index) {
        int dot = name.lastIndexOf('.');
        String suffix = " (" + index + ")";
        if (dot <= 0 || dot == name.length() - 1) {
            return name + suffix;
        }
        return name.substring(0, dot) + suffix + name.substring(dot);
    }

    private static String uniqueName(Path folder, String name) {
        String candidate = name;
        int index = 1;
        while (Files.exists(folder.resolve(candidate))) {
            candidate = withSuffix(name, index);
            index++;
        }
        return candidate;
    }

    private static String normalizeMime(String mime) {
        if (mime != null) {
            String clean = mime.trim();
            if (!clean.isEmpty() && clean.length() <= 200
                    && clean.indexOf('/') > 0 && clean.indexOf('/') < clean.length() - 1) {
                return clean;
            }
        }
        return "application/octet-stream";
    }

    private static final class Target implements ReceiveTarget {
        private final Path temp;
        private final Path destination;
        private final String displayName;
        private boolean committed;

        private Target(Path temp, Path destination, String displayName, String mimeType) {
            this.temp = temp;
            this.destination = destination;
            this.displayName = displayName;
        }

        @Override
        public OutputStream open() throws IOException {
            return new BufferedOutputStream(Files.newOutputStream(temp,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING), IO_BUFFER);
        }

        @Override
        public void commit() throws IOException {
            if (committed) {
                return;
            }
            try {
                Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, destination);
            }
            committed = true;
        }

        @Override
        public void abort() {
            if (!committed) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // Nothing else can be done.
                }
            }
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String getLocationLabel() {
            return destination.toString();
        }
    }
}
