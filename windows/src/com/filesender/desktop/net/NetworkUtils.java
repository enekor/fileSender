package com.filesender.desktop.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
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
        String fallback = null;
        for (NetworkInterface ni : interfaces) {
            String name = ni.getName() == null ? "" : ni.getName().toLowerCase();
            String address = firstIpv4(ni);
            if (address == null) {
                continue;
            }
            if (fallback == null) {
                fallback = address;
            }
            if (name.startsWith("wlan") || name.startsWith("wi-fi")
                    || name.startsWith("eth") || name.startsWith("en")) {
                return address;
            }
        }
        return fallback;
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
            for (InterfaceAddress address : addresses) {
                if (!(address.getAddress() instanceof Inet4Address)) {
                    continue;
                }
                InetAddress broadcast = address.getBroadcast();
                if (broadcast == null) {
                    broadcast = calculateBroadcast(address);
                }
                if (broadcast != null) {
                    result.add(broadcast);
                }
            }
        }
        try {
            result.add(InetAddress.getByName("255.255.255.255"));
        } catch (Exception ignored) {
            // The interface broadcasts above are still usable.
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
                } catch (Exception ignored) {
                    // Skip the interface.
                }
            }
        } catch (Exception ignored) {
            // No interfaces available.
        }
        Collections.sort(result, Comparator.comparingInt(NetworkUtils::rank));
        return result;
    }

    private static int rank(NetworkInterface ni) {
        String name = ni.getName() == null ? "" : ni.getName().toLowerCase();
        if (name.startsWith("wlan") || name.startsWith("wi-fi")) {
            return 0;
        }
        if (name.startsWith("eth") || name.startsWith("en")) {
            return 1;
        }
        return 2;
    }

    private static String firstIpv4(NetworkInterface ni) {
        try {
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address
                        && !address.isLoopbackAddress()
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
