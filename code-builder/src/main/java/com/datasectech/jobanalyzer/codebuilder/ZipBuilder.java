package com.datasectech.jobanalyzer.codebuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipBuilder {

    private final String filePath;
    private final String zipFilePath;
    private final List<File> fileList;

    public ZipBuilder(String filePath, String zipFilePath) {
        this.filePath = filePath;
        this.zipFilePath = zipFilePath;
        this.fileList = new ArrayList<>();
    }

    public void createZipFile() {
        scanFiles(new File(filePath));
        writeZipFile();
    }

    private void scanFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.add(file);
                if (file.isDirectory()) {
                    scanFiles(file);
                }
            }
        }
    }

    private void writeZipFile() {
        try {
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : fileList) {
                if (!file.isDirectory()) {
                    addToZip(file, zos);
                }
            }

            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToZip(File file, ZipOutputStream zos) throws IOException {
        byte[] bytes = new byte[1024];

        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
        }
    }
}
