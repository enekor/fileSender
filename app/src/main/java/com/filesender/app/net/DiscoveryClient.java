package com.filesender.app.net;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.filesender.app.AppConfig;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Broadcasts a discovery probe for a few seconds and reports every tablet that answers.
 */
public final class DiscoveryClient {

    private static final String TAG = "FileSenderDiscovery";
    private static final long SCAN_DURATION_MS = 6000L;
    private static final long PROBE_INTERVAL_MS = 700L;
    private static final int RECEIVE_TIMEOUT_MS = 250;

    public interface Listener {
        void onDevice(DeviceInfo device);

        void onFinished(String errorMessage);
    }

    private final Context context;
    private final Listener listener;
    private final String ownId;
    private final Set<String> seen = new HashSet<>();

    private volatile boolean running;
    private volatile boolean stopped;
    private volatile DatagramSocket socket;
    private Thread thread;
    private WifiManager.MulticastLock multicastLock;
    private boolean finished;

    public DiscoveryClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.ownId = AppConfig.deviceId(this.context);
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                scan();
            }
        }, "FileSender-DiscoveryClient");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        stopped = true;
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

    private void scan() {
        String error = null;
        DatagramSocket localSocket = null;
        try {
            acquireMulticastLock();
            localSocket = new DatagramSocket();
            localSocket.setBroadcast(true);
            localSocket.setSoTimeout(RECEIVE_TIMEOUT_MS);
            socket = localSocket;

            byte[] probe = DiscoveryProtocol.request();
            List<InetAddress> broadcasts = NetworkUtils.broadcastAddresses();
            if (broadcasts.isEmpty()) {
                error = "No hay una red WiFi disponible. Conecta las dos tablets a la misma WiFi.";
                return;
            }

            long startedAt = System.nanoTime();
            long nextProbeAt = 0L;
            long durationNanos = SCAN_DURATION_MS * 1_000_000L;
            byte[] buffer = new byte[512];

            while (running) {
                long elapsed = System.nanoTime() - startedAt;
                if (elapsed >= durationNanos) {
                    break;
                }
                if (elapsed >= nextProbeAt) {
                    for (InetAddress address : broadcasts) {
                        try {
                            localSocket.send(new DatagramPacket(
                                    probe, probe.length, address, AppConfig.UDP_PORT));
                        } catch (Exception e) {
                            Log.d(TAG, "Probe failed for " + address, e);
                        }
                    }
                    nextProbeAt = elapsed + PROBE_INTERVAL_MS * 1_000_000L;
                }

                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    localSocket.receive(packet);
                    DeviceInfo device = DiscoveryProtocol.parseReply(
                            buffer, packet.getLength(), packet.getAddress().getHostAddress());
                    if (device != null && !ownId.equals(device.id) && seen.add(device.key())) {
                        if (listener != null) {
                            listener.onDevice(device);
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                    // Normal: keep probing until the deadline.
                }
            }
        } catch (SocketException e) {
            if (running) {
                Log.w(TAG, "Discovery socket failed", e);
                error = "Error de red al buscar dispositivos. Comprueba la conexión WiFi.";
            }
        } catch (Exception e) {
            if (running) {
                Log.w(TAG, "Discovery failed", e);
                error = "No se pudo buscar dispositivos en la red WiFi.";
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

    private void acquireMulticastLock() {
        try {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("FileSender-send-discovery");
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
