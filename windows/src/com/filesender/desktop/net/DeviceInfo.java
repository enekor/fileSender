package com.filesender.desktop.net;

public final class DeviceInfo {

    public final String id;
    public final String name;
    public final String address;
    public final int port;

    public DeviceInfo(String id, String name, String address, int port) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public String key() {
        return id + "@" + address + ":" + port;
    }
}
