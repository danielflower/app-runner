package com.danielflower.apprunner;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class FileSandbox {
    public static final Logger log = LoggerFactory.getLogger(FileSandbox.class);
    public static String fullPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    private final File root;

    public FileSandbox(File root) {
        this.root = root;
    }

    public File tempDir(String name) {
        return ensureExists("temp/" + name);
    }
    public File repoDir(String name) {
        return ensureExists("repos/" + name);
    }
    public File appDir(String name, String sub) {
        return ensureExists("apps/" + name + "/" + sub);
    }
    public File appDir(String name) {
        return ensureExists("apps/" + name);
    }

    private File ensureExists(String relativePath) {
        String path = FilenameUtils.concat(fullPath(root), FilenameUtils.separatorsToSystem(relativePath));
        File f = new File(path);
        f.mkdirs();
        return f;
    }
}
