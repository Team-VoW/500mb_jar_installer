package com.voicesofwynn.installer.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static String getPreferredFileLocation(String str, String preferredName) {
        if (str == null || str.equals("")) {
            String os = System.getProperty("os.name");

            if (os.toLowerCase().contains("windows")) {
                return System.getenv("APPDATA") + "/.minecraft/mods/" + preferredName;
            } else if (os.toLowerCase().contains("mac")) {
                return System.getProperty("user.home") + "/Library/Application Support/minecraft/mods/" + preferredName;
            } else if (os.toLowerCase().contains("linux")) {
                return System.getProperty("user.home") + "/.minecraft/mods/" + preferredName;
            }

            return "./" + preferredName;
        }

        File f = new File(str);
        if (f.isDirectory()) {
            if (!str.endsWith("/")) str += "/";
            return str + preferredName;
        }

        return str;
    }

    public static void zip(File folderToZip, File zipArchive) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipArchive));
        List<File> files = new ArrayList<>(List.of(folderToZip.listFiles()));
        out.setLevel(ZipOutputStream.STORED);
        out.setLevel(0);
        while (files.size() > 0) {
            File file = files.get(0);

            if (file.isFile()) {
                String path = folderToZip.toURI().relativize(file.toURI()).getPath();
                ZipEntry e = new ZipEntry(path);
                System.out.println("packing " + path);
                e.setTime(0);
                out.putNextEntry(e);

                byte[] data = Files.readAllBytes(file.toPath());
                out.write(data, 0, data.length);
                out.closeEntry();

            } else {
                files.addAll(Arrays.asList(file.listFiles()));
            }
            files.remove(file);
        }

        out.close();
    }

    public static void unzip(String zipFilePath, String destDir) { // taken from https://www.digitalocean.com/community/tutorials/java-unzip-file-example but slightly modified
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if (!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to " + newFile.getAbsolutePath());
                //create directories for sub directories in zip
                newFile.getParentFile().mkdirs();
                Files.write(newFile.toPath(), zis.readAllBytes());
                if (Files.readAllBytes(newFile.toPath()).length == 0) {
                    newFile.delete();
                }
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteDir(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteDir(child);
            }
        }
        file.delete();
    }
}