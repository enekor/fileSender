package com.filesender.app.storage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * One file being written by the receiver. The implementation may back it with a
 * normal file (Android 7-9) or a MediaStore entry (Android 10+).
 */
public interface ReceiveTarget {

    OutputStream open() throws IOException;

    void commit() throws IOException;

    void abort();

    String getDisplayName();

    String getLocationLabel();
}
