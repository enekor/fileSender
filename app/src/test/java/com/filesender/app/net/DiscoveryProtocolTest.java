package com.filesender.app.net;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DiscoveryProtocolTest {

    @Test
    public void roundTripsDeviceReply() {
        byte[] reply = DiscoveryProtocol.reply("device-1", "SM-T280", 47822);
        DeviceInfo device = DiscoveryProtocol.parseReply(reply, reply.length, "192.168.1.50");
        assertNotNull(device);
        assertEquals("device-1", device.id);
        assertEquals("SM-T280", device.name);
        assertEquals("192.168.1.50", device.address);
        assertEquals(47822, device.port);
    }

    @Test
    public void sanitizesPipesInDeviceName() {
        byte[] reply = DiscoveryProtocol.reply("id", "Cocina|Tablet", 47822);
        DeviceInfo device = DiscoveryProtocol.parseReply(reply, reply.length, "10.0.0.2");
        assertNotNull(device);
        assertEquals("Cocina Tablet", device.name);
    }

    @Test
    public void rejectsMalformedReplies() {
        byte[] wrongPrefix = "OTHER|id|name|47822".getBytes(StandardCharsets.UTF_8);
        assertNull(DiscoveryProtocol.parseReply(wrongPrefix, wrongPrefix.length, "10.0.0.2"));

        byte[] badPort = "FILESENDER_DEVICE_V1|id|name|nope".getBytes(StandardCharsets.UTF_8);
        assertNull(DiscoveryProtocol.parseReply(badPort, badPort.length, "10.0.0.2"));

        byte[] missingParts = "FILESENDER_DEVICE_V1|id|name".getBytes(StandardCharsets.UTF_8);
        assertNull(DiscoveryProtocol.parseReply(missingParts, missingParts.length, "10.0.0.2"));
    }

    @Test
    public void requestPayloadIsStable() {
        assertEquals("FILESENDER_DISCOVER_V1",
                new String(DiscoveryProtocol.request(), StandardCharsets.UTF_8));
        assertTrue(DiscoveryProtocol.request().length > 0);
    }
}
