package com.danielflower.apprunner;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class FileSandbox {
    public static final Logger log = LoggerFactory.getLogger(FileSandbox.class);
    public static String dirPath(File samples) {
        try {
            return samples.getCanonicalPath();
        } catch (IOException e) {
            return samples.getAbsolutePath();
        }
    }

    private final File root;

    public FileSandbox(File root) {
        this.root = root;
    }

    public File tempDir(String name) {
        return ensureExists("temp/" + name);
    }
    public File appDir(String name) {
        return ensureExists("apps/" + name);
    }
    public File appDir(String name, String sub) {
        return ensureExists("apps/" + name + "/" + sub);
    }

    private File ensureExists(String relativePath) {
        String path = FilenameUtils.concat(dirPath(root), FilenameUtils.separatorsToSystem(relativePath));
        File f = new File(path);
        f.mkdirs();
        return f;
    }
}
