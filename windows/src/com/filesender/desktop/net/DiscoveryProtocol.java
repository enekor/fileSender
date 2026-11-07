package com.filesender.desktop.net;

import com.filesender.desktop.util.AppConfig;

import java.nio.charset.StandardCharsets;

public final class DiscoveryProtocol {

    public static final String REQUEST = "FILESENDER_DISCOVER_V1";
    private static final String REPLY_PREFIX = "FILESENDER_DEVICE_V1|";

    private DiscoveryProtocol() {
    }

    public static byte[] request() {
        return REQUEST.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] reply(String deviceId, String deviceName, int port) {
        String payload = REPLY_PREFIX + safe(deviceId) + "|"
                + AppConfig.cleanWireName(deviceName) + "|" + port;
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    public static DeviceInfo parseReply(byte[] data, int length, String senderAddress) {
        if (data == null || length <= 0) {
            return null;
        }
        String payload = new String(data, 0, length, StandardCharsets.UTF_8).trim();
        if (!payload.startsWith(REPLY_PREFIX)) {
            return null;
        }
        String[] parts = payload.substring(REPLY_PREFIX.length()).split("\\|", -1);
        if (parts.length != 3) {
            return null;
        }
        String id = safe(parts[0]);
        String name = AppConfig.cleanWireName(parts[1]);
        int port;
        try {
            port = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (id.isEmpty() || port <= 0 || port > 65535) {
            return null;
        }
        return new DeviceInfo(id, name, senderAddress, port);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
