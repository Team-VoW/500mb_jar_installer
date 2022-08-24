package com.voicesofwynn.installer;

import com.voicesofwynn.installer.utils.FileUtils;
import com.voicesofwynn.installer.utils.WebUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Installer {
    public static boolean install(File jarFile, InstallerOut out, String request) throws Exception {
        // create a cache dir
        out.outState("Unpacking the current jar!", 0, 1);
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
        out.outState("Connecting to server to see what changed in " + request + "!", 1, 3);
        // connect to server and get the changes
        {
            Map<String, Long> fileMap = WebUtil.getRemoteFilesFromCSV(request);

            out.outState("Scanning files!", 1, 3);

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

            int count = 0;
            for (Map.Entry<File, File> entry : move.entrySet()) {
                out.outState("Moving " + entry.getKey().getPath() + " to " + entry.getValue().getPath() + "!", count, move.size());
                System.out.println("Moving " + entry.getKey().getPath() + " to " + entry.getValue().getPath());
                entry.getValue().getParentFile().mkdirs();
                entry.getKey().renameTo(entry.getValue());
                count++;
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

            count = 0;
            for (File file : delete) {
                out.outState("Deleting " + file.getPath() + "!", count, delete.size());
                System.out.println("Deleting " + file.getPath());
                file.delete();
                count += 1;
            }

            WebUtil webUtil = new WebUtil(toGet.size());
            count = 0;
            for (String fileNeeded : toGet) {
                out.outState("Getting ready to get " + fileNeeded + "!", count, toGet.size());
                System.out.println("Asking for " + fileNeeded);

                File fileToCreate = new File(installCache.getPath() + "/" + fileNeeded);
                fileToCreate.getParentFile().mkdirs();

                fileToCreate.delete();

                webUtil.getRemoteFile(request, fileNeeded, (b) -> {
                    try {
                        FileOutputStream fOut = new FileOutputStream(fileToCreate);

                        fOut.write(b);

                        fOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                count++;
            }

            int i = webUtil.finished();
            while (i < toGet.size()) {

                String msg = "Downloaded [" + i + "/" + toGet.size() + "]!";

                System.out.println(msg);
                out.outState(msg, i, toGet.size());
                Thread.sleep(50);
                i = webUtil.finished();
            }

        }


        out.outState("Zipping it back up!", 99, 100);
        // zip it back
        FileUtils.zip(installCache, jarFile);

        FileUtils.deleteDir(installCache);

        return true;
    }
}
