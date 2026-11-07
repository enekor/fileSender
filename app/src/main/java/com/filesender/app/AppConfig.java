package com.filesender.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.format.Formatter;

import java.util.Locale;
import java.util.UUID;

public final class AppConfig {

    public static final String APP_FOLDER = "FileSender";
    public static final int UDP_PORT = 47821;
    public static final int TCP_PORT = 47822;

    private static final String PREFS = "filesender";
    private static final String PREF_DEVICE_ID = "device_id";

    private AppConfig() {
    }

    public static String deviceName() {
        String manufacturer = safe(Build.MANUFACTURER);
        String model = safe(Build.MODEL);
        String name;
        if (model.length() == 0) {
            name = "Tablet Android";
        } else if (manufacturer.length() == 0
                || model.toLowerCase(Locale.ROOT).startsWith(manufacturer.toLowerCase(Locale.ROOT))) {
            name = model;
        } else {
            name = manufacturer + " " + model;
        }
        return cleanWireName(name);
    }

    public static synchronized String deviceId(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = prefs.getString(PREF_DEVICE_ID, null);
        if (id == null || id.length() == 0) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(PREF_DEVICE_ID, id).apply();
        }
        return id;
    }

    public static String cleanWireName(String value) {
        if (value == null) {
            return "Tablet Android";
        }
        String clean = value.replace('|', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();
        if (clean.length() == 0) {
            clean = "Tablet Android";
        }
        if (clean.length() > 64) {
            clean = clean.substring(0, 64).trim();
        }
        return clean;
    }

    public static String formatFileCount(Context context, int count) {
        if (count == 1) {
            return context.getString(R.string.file_count_one);
        }
        return context.getString(R.string.file_count_many, count);
    }

    public static String formatBytes(Context context, long bytes) {
        if (bytes < 0) {
            bytes = 0;
        }
        return Formatter.formatFileSize(context, bytes);
    }

    public static int percent(long done, long total) {
        if (total <= 0) {
            return 0;
        }
        long pct = Math.round(done * 100.0d / total);
        if (pct < 0) {
            return 0;
        }
        if (pct > 100) {
            return 100;
        }
        return (int) pct;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
