package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.FileSandbox;
import com.danielflower.apprunner.io.LineConsumer;
import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.runners.AppRunner;
import com.danielflower.apprunner.runners.AppRunnerFactory;
import com.danielflower.apprunner.runners.AppRunnerFactoryProvider;
import com.danielflower.apprunner.runners.Waiter;
import io.muserver.Mutils;
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
import java.net.ServerSocket;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

public class AppManager implements AppDescription {
    public static final Logger log = LoggerFactory.getLogger(AppManager.class);
    private static final Executor deletionQueue = Executors.newSingleThreadExecutor();
    public static final int REMOTE_GIT_TIMEOUT = 300;

    public static AppManager create(String gitUrl, FileSandbox fileSandbox, String name) throws IOException, GitAPIException {
        if (!name.matches("^[A-Za-z0-9_-]+$")) {
            throw new ValidationException("The app name can only contain letters, numbers, hyphens and underscores");
        }

        File gitDir = fileSandbox.repoDir(name);
        File instanceDir = fileSandbox.tempDir(name + File.separator + "instances");
        File dataDir = fileSandbox.appDir(name, "data");
        File tempDir = fileSandbox.tempDir(name);
        File[] dirsToDelete = { gitDir, tempDir, fileSandbox.appDir(name) };

        Git git;
        boolean isNew = false;
        try {
            git = Git.open(gitDir);
            isNew = true;
        } catch (RepositoryNotFoundException e) {
            log.info("Clone app " + name + " from " + gitUrl + " to " + Mutils.fullPath(gitDir));
            git = Git.cloneRepository()
                .setURI(gitUrl)
                .setBare(false)
                .setDirectory(gitDir)
                .setTimeout(REMOTE_GIT_TIMEOUT)
                .call();
        }

        StoredConfig gitCfg = git.getRepository().getConfig();
        gitCfg.setString("remote", "origin", "url", gitUrl);
        try {
            gitCfg.save();
        } catch (IOException e) {
            throw new AppRunnerException("Error while setting remote on Git repo at " + gitDir, e);
        }
        log.info("Created app manager for " + name + " in dir " + dataDir);
        GitCommit gitCommit = getCurrentHead(name, git);
        AppManager appManager = new AppManager(name, gitUrl, git, instanceDir, dataDir, tempDir, gitCommit, dirsToDelete);
        if (isNew) {
            appManager.gitUpdateFromOrigin();
        }
        return appManager;
    }

    public static int getAFreePort() {
        try {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                return serverSocket.getLocalPort();
            }
        } catch (IOException e) {
            throw new AppRunnerException("Unable to get a port", e);
        }
    }

    private GitCommit getCurrentHead() {
        return getCurrentHead(name, git);
    }
    private static GitCommit getCurrentHead(String name, Git git) {
        GitCommit gitCommit = null;
        try {
            gitCommit = GitCommit.fromHEAD(git);
        } catch (Exception e) {
            log.warn("Could not find git commit info for " + name, e);
        }
        return gitCommit;
    }

    private void gitUpdateFromOrigin() throws GitAPIException {
        git.fetch().setRemote("origin").setTimeout(REMOTE_GIT_TIMEOUT).call();
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/master").call();
        this.contributors = getContributorsFromRepo();
    }

    private final String gitUrl;
    private final String name;
    private final Git git;
    private final File instanceDir;
    private final File dataDir;
    private final File tempDir;
    private final File[] dirsToDelete;
    private ArrayList<String> contributors;
    private final List<AppChangeListener> listeners = new ArrayList<>();
    private AppRunner currentRunner;
    private String latestBuildLog;
    private final CircularFifoQueue<String> consoleLog = new CircularFifoQueue<>(5000);
    private volatile Availability availability = Availability.unavailable("Not started");
    private volatile BuildStatus lastBuildStatus;
    private volatile BuildStatus lastSuccessfulBuildStatus;

    private AppManager(String name, String gitUrl, Git git, File instanceDir, File dataDir, File tempDir, GitCommit gitCommit, File[] dirsToDelete) {
        this.gitUrl = gitUrl;
        this.name = name;
        this.git = git;
        this.instanceDir = instanceDir;
        this.dataDir = dataDir;
        this.tempDir = tempDir;
        this.dirsToDelete = dirsToDelete;
        this.contributors = new ArrayList<>();
        this.lastBuildStatus = BuildStatus.notStarted(gitCommit);
    }

    public String name() {
        return name;
    }

    public String gitUrl() {
        return gitUrl;
    }

    @Override
    public Availability currentAvailability() {
        return availability;
    }

    @Override
    public BuildStatus lastBuildStatus() {
        return lastBuildStatus;
    }

    @Override
    public BuildStatus lastSuccessfulBuild() {
        return lastSuccessfulBuildStatus;
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

    @Override
    public File dataDir() {
        return this.dataDir;
    }

    public synchronized void stopApp() throws Exception {
        if (currentRunner != null) {
            availability = Availability.unavailable("Stopping");
            currentRunner.shutdown();
            currentRunner = null;
            availability = Availability.unavailable("Stopped");
        }
    }

    public synchronized void update(AppRunnerFactoryProvider runnerProvider, InvocationOutputHandler outputHandler) throws Exception {
        clearLogs();
        markBuildAsFetching();

        LineConsumer buildLogHandler = line -> {
            try {
                outputHandler.consumeLine(line);
            } catch (IOException ignored) {
            }
            latestBuildLog += line + LINE_SEPARATOR;
        };

        // Well this is complicated.
        // Basically, we want the build log to contain a bit of the startup, and then detach itself.
        AtomicReference<LineConsumer> buildLogHandle = new AtomicReference<>(buildLogHandler);
        LineConsumer consoleLogHandler = line -> {
            LineConsumer another = buildLogHandle.get();
            if (another != null) {
                another.consumeLine(StringUtils.stripEnd(line, "\r\n"));
            }
            synchronized (consoleLog) {
                consoleLog.add(line);
            }
        };


        buildLogHandler.consumeLine("Fetching latest changes from git...");
        File instanceDir = fetchChangesAndCreateInstanceDir();
        buildLogHandler.consumeLine("Created new instance in " + fullPath(instanceDir));

        AppRunner oldRunner = currentRunner;
        AppRunnerFactory appRunnerFactory = runnerProvider.runnerFor(name(), instanceDir);
        String runnerId = appRunnerFactory.id();
        markBuildAsStarting(runnerId);
        currentRunner = appRunnerFactory.appRunner(instanceDir);
        log.info("Using " + appRunnerFactory.id() + " for " + name);
        int port = getAFreePort();

        Map<String, String> envVarsForApp = createAppEnvVars(port, name, dataDir, tempDir);

        try (Waiter startupWaiter = Waiter.waitForApp(name, port)) {
            currentRunner.start(buildLogHandler, consoleLogHandler, envVarsForApp, startupWaiter);
        } catch (Exception e) {
            recordBuildFailure("Crashed during startup", runnerId);
            throw e;
        }
        recordBuildSuccess(runnerId);
        buildLogHandle.set(null);

        for (AppChangeListener listener : listeners) {
            listener.onAppStarted(name, new URL("http://localhost:" + port + "/" + name));
        }
        if (oldRunner != null) {
            buildLogHandler.consumeLine("Shutting down previous version");
            log.info("Shutting down previous version of " + name);
            oldRunner.shutdown();
            buildLogHandler.consumeLine("Deployment complete.");
            File oldInstanceDir = oldRunner.getInstanceDir();
            quietlyDeleteTheOldInstanceDirInTheBackground(oldInstanceDir);
        }
    }

    @Override
    public void delete() {
        git.close();
        for (File dir : dirsToDelete) {
            try {
                log.info("Deleting " + Mutils.fullPath(dir));
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                log.warn("Failed to delete " + Mutils.fullPath(dir) + " - message was " + e.getMessage(), e);
            }
        }
    }

    private void markBuildAsFetching() {
        lastBuildStatus = BuildStatus.fetching(Instant.now());
        if (!availability.isAvailable) {
            availability = Availability.unavailable("Starting");
        }
    }

    private void markBuildAsStarting(String runnerId) {
        lastBuildStatus = BuildStatus.inProgress(Instant.now(), getCurrentHead(), runnerId);
        if (!availability.isAvailable) {
            availability = Availability.unavailable("Starting");
        }
    }

    private void recordBuildSuccess(String runnerId) {
        lastBuildStatus = lastSuccessfulBuildStatus = BuildStatus.success(lastBuildStatus.startTime, Instant.now(), getCurrentHead(), runnerId);
        availability = Availability.available();
    }

    private void recordBuildFailure(String message, String runnerId) {
        lastBuildStatus = BuildStatus.failure(lastBuildStatus.startTime, Instant.now(), message, getCurrentHead(), runnerId);
        if (!availability.isAvailable) {
            availability = Availability.unavailable(message);
        }
    }

    private static void quietlyDeleteTheOldInstanceDirInTheBackground(final File instanceDir) {
        deletionQueue.execute(() -> {
            try {
                log.info("Going to delete " + fullPath(instanceDir));
                if (instanceDir.isDirectory()) {
                    FileUtils.deleteDirectory(instanceDir);
                }
                log.info("Deletion completion");
            } catch (Exception e) {
                log.info("Couldn't delete " + fullPath(instanceDir) +
                    " but it doesn't really matter as it will get deleted on next AppRunner startup.");
            }
        });
    }

    private File fetchChangesAndCreateInstanceDir() throws GitAPIException, IOException {
        try {
            gitUpdateFromOrigin();
            return copyToNewInstanceDir();
        } catch (Exception e) {
            recordBuildFailure("Could not fetch from git: " + e.getMessage(), null);
            throw e;
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
        envVarsForApp.put("TEMP", fullPath(tempDir));
        envVarsForApp.put("APP_DATA", fullPath(dataDir));
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
