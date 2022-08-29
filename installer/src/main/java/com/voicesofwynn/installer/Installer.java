package com.voicesofwynn.installer;

import com.voicesofwynn.installer.utils.FileUtils;
import com.voicesofwynn.installer.utils.WebUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Installer {
    public static void install(File jarFile, InstallerOut out, String request) throws Exception {
        // create a cache dir
        out.outState("Unpacking the current jar!", 0, 1);
        File installCache = new File(jarFile.getParent() + "/vow_installer_cache");
        if (!installCache.exists()) {
            installCache.mkdirs();
        }

        // check if jar exists
        if (jarFile.exists()) {
            // unzip it
            try {
                FileUtils.unzip(jarFile.getPath(), installCache.getPath());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Jar provided is corrupt");
                out.corruptJar();
            }
        }

        out.outState("Connecting to server to see what changed in " + request + "!", 1, 3);
        // connect to server and get the changes
        {
            Map<String, Long> fileMap = WebUtil.getRemoteFilesFromCSV(request);

            out.outState("Scanning files!", 1, 3);

            List<File> files = new LinkedList<>(Arrays.asList(installCache.listFiles()));

            Map<Long, File> disk = new HashMap<>();
            while (files.size() > 0) {
                File file = files.get(0);

                if (file.isFile()) {

                    Checksum crc32 = new CRC32();
                    crc32.update(Files.readAllBytes(file.toPath()), 0, (int) Files.size(file.toPath()));
                    long hash = crc32.getValue();

                    disk.put(hash, file);
                } else {
                    files.addAll(Arrays.asList(file.listFiles()));
                }
                files.remove(file);
            }

            int count = 0;

            List<String> toGet = new ArrayList<>();
            for (Map.Entry<String, Long> entry : fileMap.entrySet()) {
                File fl = new File(installCache.getPath() + "/" + entry.getKey());

                if (fl.isDirectory() || !fl.exists()) {
                    File f = disk.get(entry.getValue());
                    if (f != null) {
                        Files.copy(f.toPath(), fl.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        toGet.add(entry.getKey());
                    }
                } else {
                    Checksum crc32 = new CRC32();
                    crc32.update(Files.readAllBytes(fl.toPath()), 0, (int) Files.size(fl.toPath()));
                    long hash = crc32.getValue();
                    if (hash != entry.getValue()) {
                        toGet.add(entry.getKey());
                    }
                }
            }

            WebUtil webUtil = new WebUtil();
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

            files = new ArrayList<>();
            files.addAll(Arrays.asList(installCache.listFiles()));

            while (files.size() > 0) {
                File file = files.get(0);

                if (file.isFile()) {
                    String relative = installCache.toURI().relativize(file.toURI()).getPath();

                    if (!fileMap.containsKey(relative)) {
                        System.out.println("Deleting " + relative);
                        Files.delete(file.toPath());
                    }
                } else {
                    files.addAll(Arrays.asList(file.listFiles()));
                }
                files.remove(file);
            }

            for (String fileNeeded : toGet) {
                out.outState("Checking integrity of " + fileNeeded + "!", count, toGet.size());

                File fileToCheck = new File(installCache.getPath() + "/" + fileNeeded);

                Checksum crc32 = new CRC32();
                byte[] b = Files.readAllBytes(fileToCheck.toPath());
                crc32.update(b, 0, b.length);
                long hash = crc32.getValue();

                if (hash != fileMap.get(fileNeeded)) {
                    System.out.println("Hash comparision failed for " + fileNeeded);

                    System.out.println(fileMap.get(fileNeeded));

                    throw new RuntimeException("Files are incorrect.");
                }
            }

        }


        out.outState("Zipping it back up!", 99, 100);
        // zip it back
        FileUtils.zip(installCache, jarFile);

        FileUtils.deleteDir(installCache);
    }
}
