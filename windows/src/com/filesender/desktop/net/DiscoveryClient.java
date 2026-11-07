package com.filesender.desktop.net;

import com.filesender.desktop.util.AppConfig;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DiscoveryClient {

    private static final long SCAN_DURATION_MS = 6000L;
    private static final long PROBE_INTERVAL_MS = 700L;
    private static final int RECEIVE_TIMEOUT_MS = 250;

    public interface Listener {
        void onDevice(DeviceInfo device);

        void onFinished(String errorMessage);
    }

    private final Listener listener;
    private final String ownId;
    private final Set<String> seen = new HashSet<>();

    private volatile boolean running;
    private volatile boolean stopped;
    private volatile DatagramSocket socket;
    private Thread thread;
    private boolean finished;

    public DiscoveryClient(Listener listener) {
        this.listener = listener;
        this.ownId = AppConfig.deviceId();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this::scan, "FileSender-DiscoveryClient");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        stopped = true;
        running = false;
        DatagramSocket current = socket;
        socket = null;
        if (current != null) {
            current.close();
        }
    }

    private void scan() {
        String error = null;
        DatagramSocket local = null;
        try {
            local = new DatagramSocket();
            local.setBroadcast(true);
            local.setSoTimeout(RECEIVE_TIMEOUT_MS);
            socket = local;

            List<InetAddress> broadcasts = NetworkUtils.broadcastAddresses();
            if (broadcasts.isEmpty()) {
                error = "No hay una red WiFi o Ethernet disponible.";
                return;
            }

            byte[] probe = DiscoveryProtocol.request();
            byte[] buffer = new byte[512];
            long started = System.nanoTime();
            long duration = SCAN_DURATION_MS * 1_000_000L;
            long nextProbe = 0L;

            while (running) {
                long elapsed = System.nanoTime() - started;
                if (elapsed >= duration) {
                    break;
                }
                if (elapsed >= nextProbe) {
                    for (InetAddress address : broadcasts) {
                        try {
                            local.send(new DatagramPacket(probe, probe.length,
                                    address, AppConfig.UDP_PORT));
                        } catch (Exception ignored) {
                            // Try the remaining broadcast addresses.
                        }
                    }
                    nextProbe = elapsed + PROBE_INTERVAL_MS * 1_000_000L;
                }
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    local.receive(packet);
                    DeviceInfo device = DiscoveryProtocol.parseReply(
                            buffer, packet.getLength(), packet.getAddress().getHostAddress());
                    if (device != null && !ownId.equals(device.id) && seen.add(device.key())) {
                        if (listener != null) {
                            listener.onDevice(device);
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                    // Keep probing.
                }
            }
        } catch (SocketException e) {
            if (running) {
                error = "Error de red al buscar dispositivos.";
            }
        } catch (Exception e) {
            if (running) {
                error = "No se pudo buscar dispositivos en la red.";
            }
        } finally {
            running = false;
            if (local != null) {
                local.close();
            }
            socket = null;
            finish(error);
        }
    }

    private synchronized void finish(String error) {
        if (finished || stopped) {
            return;
        }
        finished = true;
        if (listener != null) {
            listener.onFinished(error);
        }
    }
}
