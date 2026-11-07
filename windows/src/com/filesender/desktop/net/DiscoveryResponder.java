package com.filesender.desktop.net;

import com.filesender.desktop.util.AppConfig;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public final class DiscoveryResponder {

    public interface Listener {
        void onDiscoveryError(String message);
    }

    private final Listener listener;
    private final String deviceId;
    private final String deviceName;
    private final int tcpPort;

    private volatile boolean running;
    private volatile DatagramSocket socket;

    public DiscoveryResponder(Listener listener) {
        this.listener = listener;
        this.deviceId = AppConfig.deviceId();
        this.deviceName = AppConfig.deviceName();
        this.tcpPort = AppConfig.TCP_PORT;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread thread = new Thread(this::serve, "FileSender-DiscoveryResponder");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        DatagramSocket current = socket;
        socket = null;
        if (current != null) {
            current.close();
        }
    }

    private void serve() {
        DatagramSocket local = null;
        try {
            local = new DatagramSocket(null);
            local.setReuseAddress(true);
            local.setBroadcast(true);
            local.bind(new InetSocketAddress(AppConfig.UDP_PORT));
            local.setSoTimeout(1000);
            socket = local;

            byte[] request = DiscoveryProtocol.request();
            byte[] reply = DiscoveryProtocol.reply(deviceId, deviceName, tcpPort);
            byte[] buffer = new byte[512];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    local.receive(packet);
                    if (packet.getLength() == request.length
                            && Arrays.equals(Arrays.copyOf(buffer, request.length), request)) {
                        local.send(new DatagramPacket(reply, reply.length,
                                packet.getAddress(), packet.getPort()));
                    }
                } catch (SocketTimeoutException ignored) {
                    // Allows a clean stop.
                }
            }
        } catch (SocketException e) {
            if (running) {
                notifyError("No se pudo abrir el puerto de búsqueda " + AppConfig.UDP_PORT
                        + ". Cierra otra instancia de FileSender.");
            }
        } catch (Exception e) {
            if (running) {
                notifyError("No se pudo iniciar la búsqueda en la red.");
            }
        } finally {
            running = false;
            if (local != null) {
                local.close();
            }
            socket = null;
        }
    }

    private void notifyError(String message) {
        if (listener != null) {
            listener.onDiscoveryError(message);
        }
    }
}
