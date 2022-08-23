package com.voicesofwynn.installer.utils;

import java.nio.ByteBuffer;

public class ByteUtils {

    public static int bytesToInt(byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }

    public static byte[] intToBytes(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

}
