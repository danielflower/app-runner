package com.danielflower.apprunner;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import com.danielflower.apprunner.mgmt.AppManager;
import com.danielflower.apprunner.mgmt.FileBasedGitRepoLoader;
import com.danielflower.apprunner.mgmt.GitRepoLoader;
import com.danielflower.apprunner.runners.RunnerProvider;
import com.danielflower.apprunner.web.ProxyMap;
import com.danielflower.apprunner.web.WebServer;
import com.danielflower.apprunner.web.v1.AppResource;
import com.danielflower.apprunner.web.v1.SystemResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.danielflower.apprunner.Config.SERVER_PORT;
import static com.danielflower.apprunner.FileSandbox.dirPath;

public class App {
    public static final Logger log = LoggerFactory.getLogger(App.class);

    private final Config config;
    private WebServer webServer;
    private AppEstate estate;
    private final AtomicBoolean startupComplete = new AtomicBoolean(false);

    public App(Config config) {
        this.config = config;
    }

    public void start() throws Exception {
        File dataDir = config.getOrCreateDir(Config.DATA_DIR);
        FileSandbox fileSandbox = new FileSandbox(dataDir);

        deleteOldTempFiles(fileSandbox.tempDir(""));

        GitRepoLoader gitRepoLoader = FileBasedGitRepoLoader.getGitRepoLoader(dataDir);
        addSampleAppIfNoAppsAlreadyThere(gitRepoLoader);

        ProxyMap proxyMap = new ProxyMap();
        int appRunnerPort = config.getInt(SERVER_PORT);

        estate = new AppEstate(
            proxyMap,
            fileSandbox,
            registerdRunnerFactories());

        for (Map.Entry<String, String> repo : gitRepoLoader.loadAll().entrySet())
            estate.addApp(repo.getValue(), repo.getKey());

        estate.addAppAddedListener(app -> gitRepoLoader.save(app.name(), app.gitUrl()));
        estate.addAppDeletedListener(app -> gitRepoLoader.delete(app.name()));

        String defaultAppName = config.get(Config.DEFAULT_APP_NAME, null);
        webServer = new WebServer(appRunnerPort, proxyMap, defaultAppName,
            new SystemResource(startupComplete), new AppResource(estate));
        webServer.start();


        deployAllAppsAsyncronously(estate);
    }

    private void deployAllAppsAsyncronously(AppEstate estate) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        estate.all().forEach(a -> executor.submit(() -> {
            StringBuilderWriter writer = new StringBuilderWriter();
            try {
                estate.update(a.name(), new OutputToWriterBridge(writer));
            } catch (Exception e) {
                log.warn("Error while starting up " + a.name() + "\nLogs:\n" + writer, e);
            }
        }));
        executor.shutdown();

        new Thread(() -> {
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
                startupComplete.set(true);
            } catch (InterruptedException e) {
                log.info("Interupted exception while awaiting startup", e);
            }
        }).start();
    }

    private void deleteOldTempFiles(File tempDir) {
        log.info("Deleting contents of temporary folder at " + dirPath(tempDir));
        try {
            FileUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
            log.warn("Failed to delete " + dirPath(tempDir), e);
        }
    }

    void addSampleAppIfNoAppsAlreadyThere(GitRepoLoader gitRepoLoader) throws Exception {
        if (gitRepoLoader.loadAll().isEmpty()) {
            String url = config.get(Config.INITIAL_APP_URL, null);
            if (StringUtils.isNotBlank(url)) {
                log.info("Adding " + url + " as an initial app");
                gitRepoLoader.save(AppManager.nameFromUrl(url), url);
            }
        }
    }

    public void shutdown() {
        log.info("Shutdown invoked");
        if (webServer != null) {
            log.info("Stopping apps");
            estate.shutdown();
            log.info("Stopping web server");
            try {
                webServer.close();
            } catch (Exception e) {
                log.info("Error while stopping", e);
            }
            log.info("Shutdown complete");
            webServer = null;
        }
    }

    private RunnerProvider registerdRunnerFactories() {
        RunnerProvider runnerProvider = new RunnerProvider(config, RunnerProvider.default_providers);
        log.info("Registered providers...\n" + runnerProvider.describeRunners());
        return runnerProvider;
    }

    public static void main(String[] args) {
        try {
            App app = new App(Config.load(args));
            app.start();
            Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
        } catch (Throwable t) {
            log.error("Error during startup", t);
            System.exit(1);
        }
    }
}
