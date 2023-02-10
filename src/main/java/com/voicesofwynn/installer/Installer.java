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
            // create the cache dir only if it doesn't exist already
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

            List<File> files = new LinkedList<>(Arrays.asList(installCache.listFiles())); // list of files from the unzipped JAR

            /* CALCULATE HASHES FROM THE LOCAL FILES */
            Map<Long, File> disk = new HashMap<>(); // contains list of files from the unzipped JAR and their hashes
            while (files.size() > 0) {
                File file = files.get(0); // get the next file to compare

                if (file.isFile()) {
                    // compute the hash of the local file
                    Checksum crc32 = new CRC32();
                    crc32.update(Files.readAllBytes(file.toPath()), 0, (int) Files.size(file.toPath()));
                    long hash = crc32.getValue();

                    disk.put(hash, file);
                } else {
                    // it's a directory, load all files from it and add them to the queue
                    files.addAll(Arrays.asList(file.listFiles()));
                }
                files.remove(file); //dequeue the processed file
            }

            int count = 0;

            /* COMPARING FILES */
            List<String> toGet = new ArrayList<>();
            for (Map.Entry<String, Long> entry : fileMap.entrySet()) { // itterate through the list of files in the remote JAR
                File fl = new File(installCache.getPath() + "/" + entry.getKey());

                if (fl.isDirectory() || !fl.exists()) { //the local file doesn't exist or we're processing a directory
                    File f = disk.get(entry.getValue());
                    if (f != null) {
                        Files.copy(f.toPath(), fl.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        // add the file to the download queue
                        toGet.add(entry.getKey());
                    }
                } else {
                    // the local file exists, compare the hashes
                    Checksum crc32 = new CRC32();
                    crc32.update(Files.readAllBytes(fl.toPath()), 0, (int) Files.size(fl.toPath()));
                    long hash = crc32.getValue();
                    if (hash != entry.getValue()) {
                        // hashes don't match, because the file changed since the current version, add it to the download queue
                        toGet.add(entry.getKey());
                    }
                }
            }

            /* DOWNLOAD THE CHANGED OR MISSING FILES */
            WebUtil webUtil = new WebUtil();
            for (String fileNeeded : toGet) {
                out.outState("Getting ready to get " + fileNeeded + "!", count, toGet.size());
                //System.out.println("Asking for " + fileNeeded);

                File fileToCreate = new File(installCache.getPath() + "/" + fileNeeded); // file to update
                fileToCreate.getParentFile().mkdirs(); // create all missing directories on the path the the file

                fileToCreate.delete(); // delete the old version of the file, that is being updated

                // download the file
                webUtil.getRemoteFile(request, fileNeeded, (b) -> {
                    try {
                        // create the file and put the contents in it
                        FileOutputStream fOut = new FileOutputStream(fileToCreate);
                        fOut.write(b);
                        fOut.close();
                    } catch (IOException e) {
                        // failed to create or write the file
                        throw new RuntimeException(e);
                    }
                });
                count++; //for progress bar, represents the number of processed files
            }

            /* UPDATE THE PROGRESS BAR AND MESSAGE PERIODICALLY */
            int i = webUtil.finished();
            while (i < toGet.size()) {

                String msg = "Downloaded [" + i + "/" + toGet.size() + "]!";

                //System.out.println(msg);
                out.outState(msg, i, toGet.size());
                Thread.sleep(50);
                i = webUtil.finished();
            }

            /* CHECK FOR FILES FROM UNPACKED JAR, THAT ARE NO LONGER NEEDED */
            files = new ArrayList<>();
            files.addAll(Arrays.asList(installCache.listFiles()));
            while (files.size() > 0) {
                File file = files.get(0);

                if (file.isFile()) {
                    String relative = installCache.toURI().relativize(file.toURI()).getPath();

                    if (!fileMap.containsKey(relative)) {
                        // file is no longer present in the version being installed, delete it
                        System.out.println("Deleting " + relative);
                        Files.delete(file.toPath());
                    }
                } else {
                    // add all files from the directory to the processing queue
                    files.addAll(Arrays.asList(file.listFiles()));
                }
                files.remove(file); //dequeue the processed file
            }

            /* CHECK THE INTEGRITY OF ALL UPDATED FILES */
            for (String fileNeeded : toGet) {
                out.outState("Checking integrity of " + fileNeeded + "!", count, toGet.size());

                File fileToCheck = new File(installCache.getPath() + "/" + fileNeeded);

                // calculate the hash of the local file
                Checksum crc32 = new CRC32();
                byte[] b = Files.readAllBytes(fileToCheck.toPath());
                crc32.update(b, 0, b.length);
                long hash = crc32.getValue();

                if (hash != fileMap.get(fileNeeded)) {
                    System.out.println("Hash comparision failed for " + fileNeeded);
                    System.out.println("File downloaded from the server has a hash of " + hash);
                    System.out.println("According to files.csv, the file should have a hash of " + fileMap.get(fileNeeded));
                    throw new RuntimeException("File " + fileNeeded + " failed the integrity check.");
                }
                /*else {
                    System.out.println("Hash comparision successful for " + fileNeeded");
                }*/
            }

        }


        out.outState("Zipping it back up!", 99, 100); // the longest 1 % of the progress bar :-)
        // zip it back
        FileUtils.zip(installCache, jarFile);

        FileUtils.deleteDir(installCache);
    }
}
