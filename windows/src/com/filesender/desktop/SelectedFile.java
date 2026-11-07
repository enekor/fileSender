package com.filesender.desktop;

import com.filesender.desktop.util.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SelectedFile {

    private final Path path;
    private final String displayName;
    private final long size;
    private final String mimeType;

    public SelectedFile(Path path) {
        this.path = path;
        this.displayName = path.getFileName().toString();
        long length;
        try {
            length = Files.size(path);
        } catch (IOException e) {
            length = -1L;
        }
        this.size = length;
        this.mimeType = AppConfig.mimeType(path);
    }

    public InputStream open() throws IOException {
        return Files.newInputStream(path);
    }

    public Path getPath() {
        return path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }
}
