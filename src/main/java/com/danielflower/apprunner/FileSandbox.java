package com.danielflower.apprunner;

import com.danielflower.apprunner.problems.AppRunnerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

public class FileSandbox {
    private final File root;

    public static String dirPath(File samples) {
        try {
            return samples.getCanonicalPath();
        } catch (IOException e) {
            return samples.getAbsolutePath();
        }
    }

    public FileSandbox(File root) {
        this.root = root;
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
