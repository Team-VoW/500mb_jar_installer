package com.voiceofwynn.installer.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketCommunication {

    private final BufferedOutputStream out;
    private final BufferedInputStream in;

    private final List<Object> values;

    public SocketCommunication(Socket sock) throws IOException {
        out = new BufferedOutputStream(sock.getOutputStream());
        in = new BufferedInputStream(sock.getInputStream());

        values = new ArrayList<>();
    }

    public Object acceptValue() throws IOException {

        switch (in.readNBytes(1)[0]) {
            case 0: {
                int length = ByteUtils.bytesToInt(in.readNBytes(4));
                return new String(in.readNBytes(length));
            }
            case 1:
                return ByteUtils.bytesToInt(in.readNBytes(4));
            case 2: {
                int length = ByteUtils.bytesToInt(in.readNBytes(4));
                return in.readNBytes(length);
            }
        }

        return null;
    }

    public void write(Object o) throws IOException {
        if (o instanceof String str) {
            byte[] bytes = str.getBytes();
            out.write(0);
            out.write(ByteUtils.intToBytes(bytes.length));
            out.write(str.getBytes());
        }
    }

    public void flush() throws IOException {
        out.flush();
    }
}
