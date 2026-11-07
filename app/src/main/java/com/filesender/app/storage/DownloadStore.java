package com.filesender.app.storage;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.filesender.app.AppConfig;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores incoming files in Downloads/FileSender.
 *
 * Android 7-9 use a normal public folder. Android 10+ use MediaStore so no broad
 * storage permission is needed on newer devices.
 */
public final class DownloadStore implements ReceiveStore {

    private static final int MAX_NAME_LENGTH = 180;
    private static final int IO_BUFFER = 64 * 1024;

    private final Context context;
    private final ContentResolver resolver;

    public DownloadStore(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    public void ensureReady() throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            return;
        }
        File folder = legacyFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta Descargas/" + AppConfig.APP_FOLDER + ".");
        }
        if (!folder.isDirectory()) {
            throw new IOException("La ruta Descargas/" + AppConfig.APP_FOLDER + " no es una carpeta.");
        }
        if (!folder.canWrite()) {
            throw new IOException("No hay permiso para escribir en Descargas/" + AppConfig.APP_FOLDER + ".");
        }
        cleanLegacyParts(folder);
    }

    @Override
    public Target create(String requestedName, String requestedMime) throws IOException {
        String name = sanitizeFileName(requestedName);
        String mime = normalizeMime(requestedMime, name);
        if (Build.VERSION.SDK_INT >= 29) {
            return Api29.create(context, resolver, name, mime);
        }
        return createLegacy(name, mime);
    }

    private Target createLegacy(String name, String mime) throws IOException {
        File folder = legacyFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta Descargas/" + AppConfig.APP_FOLDER + ".");
        }
        String uniqueName = uniqueLegacyName(folder, name);
        File destination = new File(folder, uniqueName);
        File temp = new File(folder, ".filesender-" + UUID.randomUUID() + ".part");
        if (!temp.createNewFile()) {
            throw new IOException("No se pudo crear el archivo temporal.");
        }
        return Target.legacy(context, resolver, temp, destination, uniqueName, mime);
    }

    private File legacyFolder() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloads == null) {
            downloads = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
        }
        return new File(downloads, AppConfig.APP_FOLDER);
    }

    private static void cleanLegacyParts(File folder) {
        File[] children = folder.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String name = child.getName();
            if (child.isFile() && name.startsWith(".filesender-") && name.endsWith(".part")) {
                //noinspection ResultOfMethodCallIgnored
                child.delete();
            }
        }
    }

    private static String uniqueLegacyName(File folder, String name) {
        String candidate = name;
        int index = 1;
        while (new File(folder, candidate).exists()) {
            candidate = withSuffix(name, index);
            index++;
        }
        return candidate;
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
            if (c == '?' || c == '*' || c == '<' || c == '>' || c == '|' || c == '"'
                    || c == ':') {
                clean.append('_');
            } else {
                clean.append(c);
            }
        }
        name = clean.toString().trim();
        if (name.length() == 0 || ".".equals(name) || "..".equals(name)) {
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
        }
        return name;
    }

    public static String withSuffix(String name, int index) {
        int dot = name.lastIndexOf('.');
        String suffix = " (" + index + ")";
        if (dot <= 0 || dot == name.length() - 1) {
            return name + suffix;
        }
        return name.substring(0, dot) + suffix + name.substring(dot);
    }

    private static String normalizeMime(String mime, String name) {
        if (mime != null) {
            String clean = mime.trim();
            if (clean.length() > 0 && clean.length() <= 200
                    && clean.indexOf('/') > 0 && clean.indexOf('/') < clean.length() - 1
                    && !containsControl(clean)) {
                return clean;
            }
        }
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (guessed != null) {
                return guessed;
            }
        }
        return "application/octet-stream";
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String relativeDownloadPath() {
        String base = Environment.DIRECTORY_DOWNLOADS + "/" + AppConfig.APP_FOLDER;
        return base.endsWith("/") ? base : base + "/";
    }

    public static final class Target implements ReceiveTarget {

        private final Context context;
        private final ContentResolver resolver;
        private final Uri uri;
        private final File tempFile;
        private final File finalFile;
        private final String displayName;
        private final String mimeType;
        private final boolean mediaStore;
        private boolean committed;

        private Target(Context context, ContentResolver resolver, Uri uri, File tempFile,
                       File finalFile, String displayName, String mimeType, boolean mediaStore) {
            this.context = context;
            this.resolver = resolver;
            this.uri = uri;
            this.tempFile = tempFile;
            this.finalFile = finalFile;
            this.displayName = displayName;
            this.mimeType = mimeType;
            this.mediaStore = mediaStore;
        }

        static Target legacy(Context context, ContentResolver resolver, File tempFile,
                             File finalFile, String displayName, String mimeType) {
            return new Target(context, resolver, null, tempFile, finalFile,
                    displayName, mimeType, false);
        }

        static Target mediaStore(Context context, ContentResolver resolver, Uri uri,
                                 String displayName, String mimeType) {
            return new Target(context, resolver, uri, null, null,
                    displayName, mimeType, true);
        }

        @Override
        public OutputStream open() throws IOException {
            if (mediaStore) {
                OutputStream output = resolver.openOutputStream(uri);
                if (output == null) {
                    throw new IOException("No se pudo abrir el archivo de destino.");
                }
                return new BufferedOutputStream(output, IO_BUFFER);
            }
            return new BufferedOutputStream(new FileOutputStream(tempFile), IO_BUFFER);
        }

        @Override
        public void commit() throws IOException {
            if (committed) {
                return;
            }
            if (mediaStore) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                int updated = resolver.update(uri, values, null, null);
                if (updated <= 0) {
                    throw new IOException("No se pudo finalizar el archivo en Descargas.");
                }
            } else {
                if (!tempFile.renameTo(finalFile)) {
                    throw new IOException("No se pudo guardar el archivo en Descargas.");
                }
                MediaScannerConnection.scanFile(context,
                        new String[]{finalFile.getAbsolutePath()},
                        new String[]{mimeType}, null);
            }
            committed = true;
        }

        @Override
        public void abort() {
            if (committed) {
                return;
            }
            if (mediaStore) {
                try {
                    resolver.delete(uri, null, null);
                } catch (Exception ignored) {
                    // Nothing else can be done with a pending MediaStore row.
                }
            } else if (tempFile != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String getLocationLabel() {
            return "Descargas/" + AppConfig.APP_FOLDER + "/" + displayName;
        }

        public Uri getUri() {
            return uri;
        }
    }

    @TargetApi(29)
    private static final class Api29 {

        private Api29() {
        }

        static Target create(Context context, ContentResolver resolver,
                             String name, String mime) throws IOException {
            String relative = relativeDownloadPath();
            if (relative.endsWith("/")) {
                relative = relative.substring(0, relative.length() - 1);
            }
            String uniqueName = uniqueMediaName(resolver, relative, name);
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relative);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("No se pudo crear el archivo en Descargas.");
            }
            return Target.mediaStore(context, resolver, uri, uniqueName, mime);
        }

        private static String uniqueMediaName(ContentResolver resolver, String relative, String name) {
            String candidate = name;
            int index = 1;
            while (mediaNameExists(resolver, relative, candidate)) {
                candidate = withSuffix(name, index);
                index++;
            }
            return candidate;
        }

        private static boolean mediaNameExists(ContentResolver resolver, String relative, String name) {
            String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND "
                    + MediaStore.MediaColumns.RELATIVE_PATH + " IN (?, ?)";
            String withSlash = relative.endsWith("/") ? relative : relative + "/";
            String[] args = new String[]{name, relative, withSlash};
            try (android.database.Cursor cursor = resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.MediaColumns._ID},
                    selection, args, null)) {
                return cursor != null && cursor.moveToFirst();
            } catch (Exception e) {
                return false;
            }
        }
    }
}
