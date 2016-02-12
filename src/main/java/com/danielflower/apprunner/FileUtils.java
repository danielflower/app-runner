package com.danielflower.apprunner;

import java.io.File;
import java.io.IOException;

public class FileUtils {
    public static String dirPath(File samples) {
        try {
            return samples.getCanonicalPath();
        } catch (IOException e) {
            return samples.getAbsolutePath();
        }
    }
}
