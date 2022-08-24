package com.voicesofwynn.installer.utils;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WebUtil {

    public static String[] sources = new String[]{ // don't forget the final /
            "http://localhost:8080/"
    };

    public static Map<String, remoteJar> getRemoteJarsFromCSV() throws Exception {
        Map<String, remoteJar> list = new HashMap<>();

        BufferedInputStream r = new BufferedInputStream(getHttpStream("files.csv"));

        String str = new String(r.readAllBytes());
        String[] strs = str.split("\n");

        for (int i = 1; i < strs.length; i++) {
            String s = strs[i];
            if (s.length() < 1) continue;

            String[] options = s.split(",");
            list.put(options[1], new remoteJar(options[2], options[0])); // 0: id | 1: name | 2: rname
        }

        return list;
    }

    public static byte[] getRemoteFile(String jar, String path) throws Exception {

        BufferedInputStream r = new BufferedInputStream(getHttpStream(jar + "/" + path));

        return r.readAllBytes();
    }

    public static Map<String, Long> getRemoteFilesFromCSV(String jar) throws Exception {
        Map<String, Long> list = new HashMap<>();

        BufferedInputStream r = new BufferedInputStream(getHttpStream(jar + "/files.csv"));

        String str = new String(r.readAllBytes());
        String[] strs = str.split("\n");

        for (int i = 1; i < strs.length; i++) {
            String s = strs[i];
            if (s.length() < 1) continue;

            String[] options = s.split(",");
            System.out.println(Arrays.toString(options));
            list.put(options[0], Long.parseLong(options[1])); // 0: path | 1: hash
        }

        return list;
    }

    public static InputStream getHttpStream(String address) throws IOException {

        InputStream stream = null;
        for (String source : sources) {
            try {
                URL url = new URL(source + address);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                if (con.getResponseMessage().equals("OK")) {
                    stream = con.getInputStream();
                } else {
                    throw new IOException();
                }
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return stream;
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

    public record remoteJar(String recommendedFileName, String id) {
    }

}
