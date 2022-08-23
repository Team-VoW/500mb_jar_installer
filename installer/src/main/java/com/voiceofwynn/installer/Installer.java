package com.voiceofwynn.installer;

import com.voiceofwynn.installer.utils.FileUtils;
import com.voiceofwynn.installer.utils.SocketCommunication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Installer {
    public static boolean install(File jarFile, String serverIp, int port, InstallerOut out, String request) throws IOException, NumberFormatException {
        // create a cache dir
        out.outState("Unpacking the current jar!");
        File installCache = new File(jarFile.getParent() + "/installer_cache");
        if (!installCache.exists()) {
            installCache.mkdirs();

            // check if jar exists
            if (jarFile.exists()) {
                // unzip it
                FileUtils.unzip(jarFile.getPath(), installCache.getPath());
            }
        } else {
            System.out.println("Cache dir exists which is not supposed to happen");

        }
        out.outState("Connecting to server to see what changed in " + request + "!");
        // connect to server and get the changes
        {
            Socket sock = new Socket(serverIp, port);
            SocketCommunication com = new SocketCommunication(sock);

            com.write(request);
            com.flush();

            String str = (String) com.acceptValue();
            Map<String, Long> fileMap = new HashMap<>();
            while (!str.equals("~end~")) {
                String[] strs = str.split("~/~");
                fileMap.put(strs[0], Long.parseLong(strs[1]));
                str = (String) com.acceptValue();
            }

            out.outState("Scanning files!");

            List<File> files = new ArrayList<>(List.of(installCache.listFiles()));
            List<File> delete = new ArrayList<>();
            Map<File, File> move = new HashMap<>();
            while (files.size() > 0) {
                File file = files.get(0);

                if (file.isFile()) {

                    String path = installCache.toURI().relativize(file.toURI()).getPath();

                    Checksum crc32 = new CRC32();
                    crc32.update(Files.readAllBytes(file.toPath()), 0, (int) Files.size(file.toPath()));
                    long hash = crc32.getValue();

                    if (!fileMap.containsKey(path)) {
                        if (fileMap.containsValue(hash)) {
                            for (Map.Entry<String, Long> entry : fileMap.entrySet()) {
                                if (entry.getValue() == hash) {
                                    File fl = new File(installCache.getPath() + "/" + entry.getKey());
                                    move.put(file, fl);
                                }
                            }
                        } else {
                            delete.add(file);
                        }
                    } else if (hash != fileMap.get(path)) {
                        delete.add(file);
                    }
                } else {
                    files.addAll(Arrays.asList(file.listFiles()));
                }
                files.remove(file);
            }


            List<String> toGet = new ArrayList<>();
            for (Map.Entry<String, Long> entry : fileMap.entrySet()) {
                File fl = new File(installCache.getPath() + "/" + entry.getKey());
                if (fl.isDirectory() || !fl.exists()) {
                    if (fl.isDirectory()) {
                        delete.add(fl);
                    }

                    toGet.add(entry.getKey());
                }
            }

            for (Map.Entry<File, File> entry : move.entrySet()) {
                out.outState("Moving " + entry.getKey().getPath() + " to " + entry.getValue().getPath() + "!");
                System.out.println("Moving " + entry.getKey().getPath() + " to " + entry.getValue().getPath());
                entry.getValue().getParentFile().mkdirs();
                entry.getKey().renameTo(entry.getValue());
            }

            for (File file : delete) {
                out.outState("Deleting " + file.getPath() + "!");
                System.out.println("Deleting " + file.getPath());
                file.delete();
            }

            for (String fileNeeded : toGet) {

                out.outState("Downloading " + fileNeeded + "!");
                System.out.println("Asking for " + fileNeeded);
                com.write(fileNeeded);
                com.flush();

                File fileToCreate = new File(installCache.getPath() + "/" + fileNeeded);
                fileToCreate.getParentFile().mkdirs();

                fileToCreate.delete();

                FileOutputStream fOut = new FileOutputStream(fileToCreate);

                fOut.write((byte[]) com.acceptValue());

                fOut.close();

                System.out.println("Got " + fileNeeded);

            }

        }


        out.outState("Zipping it back up!");
        // zip it back
        FileUtils.zip(installCache, jarFile);

        FileUtils.deleteDir(installCache);

        return true;
    }
}
