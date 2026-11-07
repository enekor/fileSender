package com.filesender.app.net;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.filesender.app.AppConfig;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Answers UDP discovery probes while the receive screen is open.
 */
public final class DiscoveryResponder {

    private static final String TAG = "FileSenderDiscovery";
    private static final int MAX_PACKET = 512;

    public interface Listener {
        void onDiscoveryError(String message);
    }

    private final Context context;
    private final int tcpPort;
    private final Listener listener;
    private final String deviceId;
    private final String deviceName;

    private volatile boolean running;
    private volatile DatagramSocket socket;
    private Thread thread;
    private WifiManager.MulticastLock multicastLock;

    public DiscoveryResponder(Context context, int tcpPort, Listener listener) {
        this.context = context.getApplicationContext();
        this.tcpPort = tcpPort;
        this.listener = listener;
        this.deviceId = AppConfig.deviceId(this.context);
        this.deviceName = AppConfig.deviceName();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                serve();
            }
        }, "FileSender-DiscoveryResponder");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        DatagramSocket current = socket;
        socket = null;
        if (current != null) {
            try {
                current.close();
            } catch (Exception ignored) {
                // Nothing to do.
            }
        }
        releaseMulticastLock();
    }

    private void serve() {
        DatagramSocket localSocket = null;
        try {
            acquireMulticastLock();
            localSocket = new DatagramSocket(null);
            localSocket.setReuseAddress(true);
            localSocket.bind(new InetSocketAddress(AppConfig.UDP_PORT));
            socket = localSocket;

            byte[] request = DiscoveryProtocol.request();
            byte[] reply = DiscoveryProtocol.reply(deviceId, deviceName, tcpPort);
            byte[] buffer = new byte[MAX_PACKET];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                localSocket.receive(packet);
                if (!running) {
                    break;
                }
                int length = packet.getLength();
                if (length != request.length) {
                    continue;
                }
                boolean matches = true;
                for (int i = 0; i < request.length; i++) {
                    if (buffer[i] != request[i]) {
                        matches = false;
                        break;
                    }
                }
                if (!matches) {
                    continue;
                }
                DatagramPacket response = new DatagramPacket(
                        reply, reply.length, packet.getAddress(), packet.getPort());
                localSocket.send(response);
            }
        } catch (BindException e) {
            if (running) {
                notifyError("No se pudo abrir el puerto de búsqueda "
                        + AppConfig.UDP_PORT + ". Cierra otras apps de transferencia y reinténtalo.");
            }
        } catch (SocketException e) {
            if (running) {
                notifyError("No se pudo iniciar la búsqueda en la red WiFi.");
            }
        } catch (Exception e) {
            if (running) {
                Log.w(TAG, "Discovery responder stopped", e);
                notifyError("Error de red al buscar dispositivos.");
            }
        } finally {
            running = false;
            if (localSocket != null) {
                try {
                    localSocket.close();
                } catch (Exception ignored) {
                    // Nothing to do.
                }
            }
            socket = null;
            releaseMulticastLock();
        }
    }

    private void notifyError(final String message) {
        if (listener != null) {
            listener.onDiscoveryError(message);
        }
    }

    private void acquireMulticastLock() {
        try {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("FileSender-receive-discovery");
                multicastLock.setReferenceCounted(false);
                multicastLock.acquire();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not acquire multicast lock", e);
        }
    }

    private void releaseMulticastLock() {
        WifiManager.MulticastLock lock = multicastLock;
        multicastLock = null;
        if (lock != null) {
            try {
                if (lock.isHeld()) {
                    lock.release();
                }
            } catch (Exception ignored) {
                // Nothing to do.
            }
        }
    }
}
