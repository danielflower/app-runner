package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.problems.InvalidConfigException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class FileBasedGitRepoLoader implements GitRepoLoader {

    private final File file;

    public FileBasedGitRepoLoader(File file) {
        if (!file.isFile()) {
            throw new InvalidConfigException("Could not find git repo list at " + dirPath(file));
        }
        this.file = file;
    }

    @Override
    public List<String> loadAll() throws Exception {
        return FileUtils.readLines(file);
    }
}
