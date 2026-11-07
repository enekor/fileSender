package com.filesender.app.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NetworkUtils {

    private NetworkUtils() {
    }

    public static String localIpv4Address() {
        List<NetworkInterface> interfaces = interfaces();
        for (NetworkInterface ni : interfaces) {
            String name = ni.getName() == null ? "" : ni.getName();
            if (!(name.startsWith("wlan") || name.startsWith("eth") || name.startsWith("ap"))) {
                continue;
            }
            String address = firstIpv4(ni);
            if (address != null) {
                return address;
            }
        }
        for (NetworkInterface ni : interfaces) {
            String address = firstIpv4(ni);
            if (address != null) {
                return address;
            }
        }
        return null;
    }

    public static List<InetAddress> broadcastAddresses() {
        Set<InetAddress> result = new LinkedHashSet<>();
        for (NetworkInterface ni : interfaces()) {
            List<InterfaceAddress> addresses;
            try {
                addresses = ni.getInterfaceAddresses();
            } catch (Exception e) {
                continue;
            }
            for (InterfaceAddress ifAddress : addresses) {
                if (!(ifAddress.getAddress() instanceof Inet4Address)) {
                    continue;
                }
                InetAddress broadcast = ifAddress.getBroadcast();
                if (broadcast == null) {
                    broadcast = calculateBroadcast(ifAddress);
                }
                if (broadcast != null) {
                    result.add(broadcast);
                }
            }
        }
        try {
            result.add(InetAddress.getByName("255.255.255.255"));
        } catch (Exception ignored) {
            // At least the interface broadcasts above are still usable.
        }
        return new ArrayList<>(result);
    }

    private static List<NetworkInterface> interfaces() {
        List<NetworkInterface> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            if (enumeration == null) {
                return result;
            }
            while (enumeration.hasMoreElements()) {
                NetworkInterface ni = enumeration.nextElement();
                try {
                    if (ni.isUp() && !ni.isLoopback()) {
                        result.add(ni);
                    }
                } catch (SocketException ignored) {
                    // Skip an interface that cannot be inspected.
                }
            }
        } catch (SocketException ignored) {
            // No network interfaces available.
        }
        Collections.sort(result, new Comparator<NetworkInterface>() {
            @Override
            public int compare(NetworkInterface left, NetworkInterface right) {
                return Integer.compare(rank(left), rank(right));
            }

            private int rank(NetworkInterface ni) {
                String name = ni.getName() == null ? "" : ni.getName();
                if (name.startsWith("wlan")) {
                    return 0;
                }
                if (name.startsWith("eth")) {
                    return 1;
                }
                return 2;
            }
        });
        return result;
    }

    private static String firstIpv4(NetworkInterface ni) {
        try {
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()) {
                    return address.getHostAddress();
                }
            }
        } catch (Exception ignored) {
            // Try the next interface.
        }
        return null;
    }

    private static InetAddress calculateBroadcast(InterfaceAddress address) {
        InetAddress local = address.getAddress();
        int prefix = address.getNetworkPrefixLength();
        if (!(local instanceof Inet4Address) || prefix <= 0 || prefix >= 32) {
            return null;
        }
        byte[] bytes = local.getAddress();
        int value = ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
        int mask = -1 << (32 - prefix);
        int broadcast = (value & mask) | ~mask;
        byte[] result = new byte[]{
                (byte) (broadcast >>> 24),
                (byte) (broadcast >>> 16),
                (byte) (broadcast >>> 8),
                (byte) broadcast
        };
        try {
            return InetAddress.getByAddress(result);
        } catch (Exception e) {
            return null;
        }
    }
}
