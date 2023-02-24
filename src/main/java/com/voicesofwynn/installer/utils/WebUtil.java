package com.voicesofwynn.installer.utils;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.voicesofwynn.installer.utils.ByteUtils.readAllBytes;

public class WebUtil {

    public static final int THREAD_AMOUNT = 8;
    public static String[] sources = new String[]{ // don't forget the final /
            "https://raw.githubusercontent.com/Team-VoW/updater-data/",
            "http://voicesofwynn.com/files/updater-data/",
            "https://raw.githubusercontent.com/Team-VoW/updater-data/",
            "http://voicesofwynn.com/files/updater-data/"
    };

    private final ThreadPoolExecutor es;

    public WebUtil() {
        es = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_AMOUNT);
    }

    public static Map<String, remoteJar> getRemoteJarsFromCSV() throws Exception {
        Map<String, remoteJar> list = new LinkedHashMap<>();

        BufferedInputStream r = new BufferedInputStream(getHttpStream("main/versions.csv"));

        String str = new String(readAllBytes(r));
        String[] strs = str.split("\n");

        //System.out.println(str);

        for (int i = 1; i < strs.length; i++) {
            String s = strs[i];
            if (s.length() < 1) continue;

            String[] options = s.split(",");
            options[2] = options[2].replaceAll("[^a-zA-Z\\d._$%@#~()*&^!?§]", "");
            list.put(options[1], new remoteJar(options[2], options[0])); // 0: id | 1: name | 2: rname
        }

        return list;
    }

    public static Map<String, Long> getRemoteFilesFromCSV(String jar) throws Exception {
        Map<String, Long> list = new HashMap<>();

        System.out.println("Trying to get " + jar + "/files.csv.");
        BufferedInputStream r = new BufferedInputStream(getHttpStream(jar + "/files.csv"));

        String str = new String(readAllBytes(r));
        String[] strs = str.split("\n");


        for (int i = 1; i < strs.length; i++) {
            String s = strs[i];
            if (s.length() < 1) continue;

            String[] options = s.split(",");
            //System.out.println(Arrays.toString(options));
            list.put(options[0], Long.parseLong(options[1])); // 0: path | 1: hash
        }

        return list;
    }

    public static InputStream getHttpStream(String address) throws IOException {
        address = address.replace(" ", "%20"); //Replace spaces in the filename
        InputStream stream = null;
        try {
            int i = 0;
            for (String source : sources) {
                try {
                    URL url = new URL(source + address);
                    System.out.println("\t\tConnecting to " + source + address + ".");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(2000);

                    // sort out redirection response, send a second request to the new location
                    String redirect = con.getHeaderField("Location");
                    if (redirect != null) {
                        System.out.println("Redirection!");
                        System.out.println("\t\tConnecting to " + redirect + "... ");
                        con = (HttpURLConnection) new URL(redirect).openConnection();
                        con.setConnectTimeout(2000);
                    }

                    String re = con.getResponseMessage();
                    if (re.equals("OK")) {
                        stream = con.getInputStream();
                        
						// replace primary source with this one, because it works the best for now
                        String zero = sources[i];
                        sources[i] = sources[0];
                        sources[0] = zero;
                    } else {
                        System.out.println(re + " | " + url);
                        throw new IOException();
                    }
                    break; // no need to try other sources
                } catch (IOException e) {
                    //e.printStackTrace();
                }
                i++;
            }
        } catch (ConcurrentModificationException e) {
			System.out.println("\t\tFailed.");
            //e.printStackTrace();
            stream = getHttpStream(address);
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

    public int finished() {
        return (int) es.getCompletedTaskCount();
    }

    public void getRemoteFile(String jar, String path, remoteFileGot rfg) {
        es.submit(
                () -> {
                    try {
                        InputStream s = getHttpStream(jar + "/" + path);

                        rfg.run(readAllBytes(s));
                    } catch (Exception e) {
                        rfg.run(null);
                    }
                }
        );

    }

    public interface remoteFileGot {
        void run(byte[] contents);
    }

    public static class remoteJar {
        public String recommendedFileName_, id_;
        public remoteJar(String recommendedFileName, String id) {
            this.recommendedFileName_ = recommendedFileName;
            this.id_ = id;
        }

        public String recommendedFileName() {
            return recommendedFileName_;
        }

        public String id() {
            return id_;
        }
    }

}
