package com.voiceofwynn.installer.utils;

import java.awt.*;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class InstallerUiUsedUtils {

    public record remoteJar(String recommendedFileName, long hash) {
    }

    public static Map<String, remoteJar> getRemoteJars(String serverIp, int port) throws IOException {
        Map<String, remoteJar> list = new HashMap<>();

        Socket sock = new Socket(serverIp, port);
        SocketCommunication com = new SocketCommunication(sock);
        com.write("list");
        com.flush();

        String str = (String) com.acceptValue();
        while (!str.equals("+end*")) {
            String[] strs = str.split("~=/");
            list.put(strs[0], new remoteJar(strs[1], Long.parseLong(strs[2])));
            str = (String) com.acceptValue();
        }

        return list;
    }

    public static boolean openWebpage(URI uri) {
        Desktop desktop = Desktop.getDesktop();
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
