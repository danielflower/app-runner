package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.FileSandbox;
import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.runners.MavenRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.StoredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AppManager implements AppDescription {
    public static final Logger log = LoggerFactory.getLogger(AppManager.class);

    private final String gitUrl;
    private final String name;
    private final Git git;
    private final File instanceDir;
    private MavenRunner currentRunner = null;
    private List<AppChangeListener> listeners = new ArrayList<>();

    private AppManager(String name, String gitUrl, Git git, File instanceDir) {
        this.gitUrl = gitUrl;
        this.name = name;
        this.git = git;
        this.instanceDir = instanceDir;
    }

    @Override
    public String name() {
        return name;
    }
    @Override
    public String gitUrl() {
        return gitUrl;
    }

    public static AppManager create(String gitUrl, FileSandbox fileSandbox) {
        String name = StringUtils.removeEndIgnoreCase(StringUtils.removeEnd(gitUrl, "/"), ".git");
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        File root = fileSandbox.appDir(name);
        File gitDir = fileSandbox.appDir(name, "repo");
        File instanceDir = fileSandbox.appDir(name, "instances");

        Git git;
        try {
            try {
                git = Git.open(gitDir);
            } catch (RepositoryNotFoundException e) {
                git = Git.cloneRepository()
                    .setURI(gitUrl)
                    .setBare(false)
                    .setDirectory(gitDir)
                    .call();
            }
        } catch (IOException | GitAPIException e) {
            throw new AppRunnerException("Could not open or create git repo at " + gitDir, e);
        }
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", gitUrl);
        try {
            config.save();
        } catch (IOException e) {
            throw new AppRunnerException("Error while setting remove on Git repo at " + gitDir, e);
        }
        log.info("Created app manager for " + name + " in dir " + root);
        return new AppManager(name, gitUrl, git, instanceDir);
    }

    public synchronized void stopApp() throws Exception {
        if (currentRunner != null) {
            currentRunner.shutdown();
            currentRunner = null;
        }
    }

    @Override
    public synchronized void update(Writer writer) throws Exception {
        git.pull().setRemote("origin").call();
        File id = copyToNewInstanceDir();
        currentRunner = new MavenRunner(id);
        int port = getAFreePort();
        currentRunner.start(port);
        for (AppChangeListener listener : listeners) {
            listener.onAppStarted(name, new URL("http://localhost:" + port + "/" + name));
        }
    }


    private int getAFreePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    private File copyToNewInstanceDir() throws IOException {
        File dest = new File(instanceDir, String.valueOf(System.currentTimeMillis()));
        dest.mkdir();
        FileUtils.copyDirectory(git.getRepository().getWorkTree(), dest,
            pathname -> !pathname.getName().equals(".git"));
        return dest;
    }

    public void addListener(AppChangeListener appChangeListener) {
        listeners.add(appChangeListener);
    }

    public interface AppChangeListener {
        void onAppStarted(String name, URL newUrl);
    }
}
