package com.danielflower.apprunner.io;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zippy {
    public static void zipDirectory(File sourceDir, File targetZip) throws IOException {
        try (FileOutputStream target = new FileOutputStream(targetZip)) {
            zipDirectory(sourceDir, target);
        }
    }

    public static void zipDirectory(File sourceDir, OutputStream target) throws IOException {
        try (ZipOutputStream zout = new ZipOutputStream(target)) {
            zipSubDirectory("", sourceDir, zout);
        }
    }

    private static void zipSubDirectory(String basePath, File dir, ZipOutputStream zout) throws IOException {
        byte[] buffer = new byte[4096];
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String path = basePath + file.getName() + "/";
                zout.putNextEntry(new ZipEntry(path));
                zipSubDirectory(path, file, zout);
                zout.closeEntry();
            } else {
                try (FileInputStream fin = new FileInputStream(file)) {
                    zout.putNextEntry(new ZipEntry(basePath + file.getName()));
                    int length;
                    while ((length = fin.read(buffer)) > 0) {
                        zout.write(buffer, 0, length);
                    }
                    zout.closeEntry();
                }
            }
        }
    }
}
