package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.FileSandbox;
import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.runners.AppRunner;
import com.danielflower.apprunner.runners.RunnerProvider;
import com.danielflower.apprunner.runners.Waiter;
import com.danielflower.apprunner.web.WebServer;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class AppManager implements AppDescription {
    public static final Logger log = LoggerFactory.getLogger(AppManager.class);

    public static AppManager create(String gitUrl, FileSandbox fileSandbox, String name) throws IOException, GitAPIException {
        File root = fileSandbox.appDir(name);
        File gitDir = fileSandbox.appDir(name, "repo");
        File instanceDir = fileSandbox.tempDir(name + File.separator + "instances");
        File dataDir = fileSandbox.appDir(name, "data");
        File tempDir = fileSandbox.tempDir(name);

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

        StoredConfig gitCfg = git.getRepository().getConfig();
        gitCfg.setString("remote", "origin", "url", gitUrl);
        try {
            gitCfg.save();
        } catch (IOException e) {
            throw new AppRunnerException("Error while setting remote on Git repo at " + gitDir, e);
        }
        log.info("Created app manager for " + name + " in dir " + root);
        return new AppManager(name, gitUrl, git, instanceDir, dataDir, tempDir);
    }

    private final String gitUrl;
    private final String name;
    private final Git git;
    private final File instanceDir;
    private final File dataDir;
    private final File tempDir;
    private ArrayList<String> contributors;
    private final List<AppChangeListener> listeners = new ArrayList<>();
    private AppRunner currentRunner;
    private String latestBuildLog;
    private final CircularFifoQueue<String> consoleLog = new CircularFifoQueue<>(5000);

    private AppManager(String name, String gitUrl, Git git, File instanceDir, File dataDir, File tempDir) {
        this.gitUrl = gitUrl;
        this.name = name;
        this.git = git;
        this.instanceDir = instanceDir;
        this.dataDir = dataDir;
        this.tempDir = tempDir;
        this.contributors = new ArrayList<>();
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

    public String latestConsoleLog() {
        synchronized (consoleLog) {
            return consoleLog.stream().collect(Collectors.joining());
        }
    }

    public ArrayList<String> contributors() {
        return contributors;
    }

    public synchronized void stopApp() throws Exception {
        if (currentRunner != null) {
            currentRunner.shutdown();
            currentRunner = null;
        }
    }

    public synchronized void update(RunnerProvider runnerProvider, InvocationOutputHandler outputHandler) throws Exception {
        clearLogs();
        this.contributors = getContributorsFromRepo();

        InvocationOutputHandler buildLogHandler = line -> {
            outputHandler.consumeLine(line);
            latestBuildLog += line + "\n";
        };

        // Well this is complicated.
        // Basically, we want the build log to contain a bit of the startup, and then detach itself.
        AtomicReference<InvocationOutputHandler> buildLogHandle = new AtomicReference<>(buildLogHandler);
        InvocationOutputHandler consoleLogHandler = line -> {
            InvocationOutputHandler another = buildLogHandle.get();
            if (another != null) {
                another.consumeLine(StringUtils.stripEnd(line, "\r\n"));
            }
            synchronized (consoleLog) {
                consoleLog.add(line);
            }
        };


        buildLogHandler.consumeLine("Fetching latest changes from git...");
        git.fetch().setRemote("origin").call();
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/master").call();
        File id = copyToNewInstanceDir();
        buildLogHandler.consumeLine("Created new instance in " + dirPath(id));

        AppRunner oldRunner = currentRunner;
        currentRunner = runnerProvider.runnerFor(name(), id);
        int port = WebServer.getAFreePort();

        Map<String, String> envVarsForApp = createAppEnvVars(port, name, dataDir, tempDir);

        try (Waiter startupWaiter = Waiter.waitForApp(name, port)) {
            currentRunner.start(buildLogHandler, consoleLogHandler, envVarsForApp, startupWaiter);
        }

        buildLogHandle.set(null);

        for (AppChangeListener listener : listeners) {
            listener.onAppStarted(name, new URL("http://localhost:" + port + "/" + name));
        }
        if (oldRunner != null) {
            buildLogHandler.consumeLine("Shutting down previous version");
            log.info("Shutting down previous version of " + name);
            oldRunner.shutdown();
            buildLogHandler.consumeLine("Deployment complete.");



            // TODO: delete old instance dir
        }
    }

    private ArrayList<String> getContributorsFromRepo() {
        ArrayList<String> contributors = new ArrayList<>();
        try {
            // get authors
            Iterable<RevCommit> commits = git.log().all().call();
            for (RevCommit commit : commits) {
                String author = commit.getAuthorIdent().getName();
                if (!contributors.contains(author)) {
                    contributors.add(author);
                }
            }
            log.info("getting the contributors " + contributors);

        } catch (Exception e) {
            log.warn("Failed to get authors from repo: " + e.getMessage());
        }
        return contributors;
    }

    private void clearLogs() {
        latestBuildLog = "";
        synchronized (consoleLog) {
            consoleLog.clear();
        }
    }

    public static Map<String, String> createAppEnvVars(int port, String name, File dataDir, File tempDir) {
        HashMap<String, String> envVarsForApp = new HashMap<>(System.getenv());
        envVarsForApp.put("APP_PORT", String.valueOf(port));
        envVarsForApp.put("APP_NAME", name);
        envVarsForApp.put("APP_ENV", "prod");
        envVarsForApp.put("TEMP", dirPath(tempDir));
        envVarsForApp.put("APP_DATA", dirPath(dataDir));
        return envVarsForApp;
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
