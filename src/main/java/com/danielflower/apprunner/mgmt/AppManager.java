package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.FileSandbox;
import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.runners.MavenRunner;
import com.danielflower.apprunner.web.WebServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.StoredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AppManager implements AppDescription {
    public static final Logger log = LoggerFactory.getLogger(AppManager.class);

    public static AppManager create(String gitUrl, FileSandbox fileSandbox, File javaHome, String name) {
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
        return new AppManager(name, gitUrl, git, instanceDir, javaHome);
    }

    private final String gitUrl;
    private final String name;
    private final Git git;
    private final File instanceDir;
    private final File javaHome;
    private final List<AppChangeListener> listeners = new ArrayList<>();
    private MavenRunner currentRunner;
    private String latestBuildLog;

    private AppManager(String name, String gitUrl, Git git, File instanceDir, File javaHome) {
        this.gitUrl = gitUrl;
        this.name = name;
        this.git = git;
        this.instanceDir = instanceDir;
        this.javaHome = javaHome;
    }

    public String name() {
        return name;
    }

    public String gitUrl() {
        return gitUrl;
    }

    public String latestBuildLog() {
        return latestBuildLog;
    }

    public synchronized void stopApp() throws Exception {
        if (currentRunner != null) {
            currentRunner.shutdown();
            currentRunner = null;
        }
    }

    public synchronized void update(InvocationOutputHandler outputHandler) throws GitAPIException, IOException {
        latestBuildLog = "";

        InvocationOutputHandler actualOutputHandler = line -> {
            outputHandler.consumeLine(line);
            latestBuildLog += line + "\n";
        };

        git.pull().setRemote("origin").call();
        File id = copyToNewInstanceDir();
        MavenRunner oldRunner = currentRunner;
        currentRunner = new MavenRunner(id, javaHome);
        int port = WebServer.getAFreePort();
        currentRunner.start(port, actualOutputHandler, name());
        for (AppChangeListener listener : listeners) {
            listener.onAppStarted(name, new URL("http://localhost:" + port + "/" + name));
        }
        if (oldRunner != null) {
            log.info("Shutting down previous version of " + name);
            oldRunner.shutdown();

            // TODO: delete old instance dir
        }
    }

    public void addListener(AppChangeListener appChangeListener) {
        listeners.add(appChangeListener);
    }

    public interface AppChangeListener {
        void onAppStarted(String name, URL newUrl);
    }

    private File copyToNewInstanceDir() throws IOException {
        File dest = new File(instanceDir, String.valueOf(System.currentTimeMillis()));
        dest.mkdir();
        FileUtils.copyDirectory(git.getRepository().getWorkTree(), dest, pathname -> !pathname.getName().equals(".git"));
        return dest;
    }

    public static String nameFromUrl(String gitUrl) {
        String name = StringUtils.removeEndIgnoreCase(StringUtils.removeEnd(gitUrl, "/"), ".git");
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        return name;
    }
}
