package com.filesender.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class SelectedFile {

    private final Uri uri;
    private final String displayName;
    private final long size;
    private final String mimeType;
    private final File localFile;

    private SelectedFile(Uri uri, String displayName, long size, String mimeType, File localFile) {
        this.uri = uri;
        this.displayName = displayName;
        this.size = size;
        this.mimeType = mimeType;
        this.localFile = localFile;
    }

    /**
     * Test-only source backed by a regular file. App code always uses content URIs.
     */
    static SelectedFile forLocalFile(File file, long declaredSize) {
        return new SelectedFile(null, file.getName(), declaredSize,
                guessMimeType(file.getName()), file);
    }

    public static SelectedFile from(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        ContentResolver resolver = context.getApplicationContext().getContentResolver();
        String name = null;
        long size = -1L;
        String mimeType = null;

        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri,
                        new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                        null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex);
                    }
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception ignored) {
                // Metadata is optional; the stream can still be transferred.
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            try {
                mimeType = resolver.getType(uri);
            } catch (Exception ignored) {
                // Fall back to the extension below.
            }
            if (size < 0) {
                AssetFileDescriptor descriptor = null;
                try {
                    descriptor = resolver.openAssetFileDescriptor(uri, "r");
                    if (descriptor != null && descriptor.getLength() >= 0) {
                        size = descriptor.getLength();
                    }
                } catch (Exception ignored) {
                    // Unknown size will use chunked transfer.
                } finally {
                    if (descriptor != null) {
                        try {
                            descriptor.close();
                        } catch (IOException ignored) {
                            // Nothing to do.
                        }
                    }
                }
            }
        } else if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            name = file.getName();
            size = file.isFile() ? file.length() : -1L;
        }

        if (name == null || name.trim().length() == 0) {
            name = uri.getLastPathSegment();
        }
        if (name == null || name.trim().length() == 0) {
            name = "archivo";
        }
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.length() == 0) {
            name = "archivo";
        }
        if (mimeType == null || mimeType.length() == 0) {
            mimeType = guessMimeType(name);
        }
        return new SelectedFile(uri, name, size, mimeType, null);
    }

    public InputStream open(Context context) throws IOException {
        if (localFile != null) {
            return new FileInputStream(localFile);
        }
        if ("file".equals(uri.getScheme())) {
            return new FileInputStream(new File(uri.getPath()));
        }
        InputStream stream = context.getApplicationContext()
                .getContentResolver().openInputStream(uri);
        if (stream == null) {
            throw new IOException("No se pudo abrir el archivo.");
        }
        return stream;
    }

    public Uri getUri() {
        return uri;
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

    private static String guessMimeType(String name) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            MimeTypeMap map = MimeTypeMap.getSingleton();
            String type = map == null ? null : map.getMimeTypeFromExtension(extension);
            if (type != null) {
                return type;
            }
        }
        return "application/octet-stream";
    }
}
