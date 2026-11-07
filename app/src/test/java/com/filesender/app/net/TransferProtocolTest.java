package com.filesender.app.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransferProtocolTest {

    @Test
    public void protocolValuesAreSane() {
        assertTrue(TransferProtocol.MAGIC != 0);
        assertEquals(1, TransferProtocol.VERSION);
        assertTrue(TransferProtocol.MAX_FILES > 0);
        assertTrue(TransferProtocol.BUFFER_SIZE >= 8192);
        assertTrue(TransferProtocol.MAX_FILE_NAME > 0);
    }
}
