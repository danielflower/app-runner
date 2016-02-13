package com.danielflower.apprunner;

import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.problems.InvalidConfigException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class Config {
    public static final String SERVER_PORT = "appserver.port";
    public static final String REPO_FILE_PATH = "appserver.git.repo.list.path";
    public static final String STASH_PROJECT_URL = "appserver.git.stash.project.url";
    public static final String DATA_DIR = "appserver.data.dir";
    private final Map<String, String> raw;

    public Config(Map<String, String> raw) {
        this.raw = raw;
    }

    public String get(String name, String defaultVal) {
        return raw.getOrDefault(name, defaultVal);
    }

    public String get(String name) {
        String s = get(name, null);
        if (s == null) {
            throw new InvalidConfigException("Missing config item: " + name);
        }
        return s;
    }

    public int getInt(String name) {
        String s = get(name);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("Could not convert " + name + "=" + s + " to an integer");
        }
    }

    public File getDir(String name) {
        File f = new File(get(name));
        try {
            FileUtils.forceMkdir(f);
        } catch (IOException e) {
            throw new AppRunnerException("Could not create " + dirPath(f));
        }
        return f;
    }
}
