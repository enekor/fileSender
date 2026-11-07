package com.filesender.desktop.storage;

import java.io.IOException;

public interface ReceiveStore {

    ReceiveTarget create(String requestedName, String mimeType) throws IOException;
}
