package com.filesender.desktop.storage;

import java.io.IOException;
import java.io.OutputStream;

public interface ReceiveTarget {

    OutputStream open() throws IOException;

    void commit() throws IOException;

    void abort();

    String getDisplayName();

    String getLocationLabel();
}
