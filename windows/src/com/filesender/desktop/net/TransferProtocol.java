package com.filesender.desktop.net;

public final class TransferProtocol {

    public static final int MAGIC = 0x46533102;
    public static final int VERSION = 1;
    public static final int MAX_FILES = 500;
    public static final int MAX_FILE_NAME = 200;
    public static final int MAX_MIME_LENGTH = 200;
    public static final int ACK_OK = 0x4F4B;
    public static final int BUFFER_SIZE = 64 * 1024;

    private TransferProtocol() {
    }
}
