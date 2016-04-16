package com.danielflower.apprunner.mgmt;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileBasedGitRepoLoader implements GitRepoLoader {

    public static GitRepoLoader getGitRepoLoader(File dataDir) throws IOException {
        File dataStore = new File(dataDir, "repos.properties");
        FileUtils.touch(dataStore);

        Properties properties = new Properties();
        try (FileReader reader = new FileReader(dataStore)) {
            properties.load(reader);
        }

        return new FileBasedGitRepoLoader(dataStore, properties);
    }

    private final File file;
    private final Properties properties;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private FileBasedGitRepoLoader(File file, Properties properties) {
        this.file = file;
        this.properties = properties;
    }

    public Map<String, String> loadAll() {
        Lock l = lock.readLock();
        l.lock();
        try {
            Map<String,String> all = new HashMap<>();
            for (String key : properties.stringPropertyNames()) {
                all.put(key, properties.getProperty(key));
            }
            return all;
        } finally {
            l.unlock();
        }
    }

    public void save(String name, String gitUrl) throws IOException {
        Lock l = lock.writeLock();
        l.lock();
        try {
            properties.setProperty(name, gitUrl);
            saveToDisk();
        } finally {
            l.unlock();
        }
    }

    private void saveToDisk() throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            properties.store(writer, "Saved by " + getClass().getSimpleName());
        }
    }

    public void delete(String name) throws IOException {
        Lock l = lock.writeLock();
        l.lock();
        try {
            properties.remove(name);
            saveToDisk();
        } finally {
            l.unlock();
        }
    }
}
