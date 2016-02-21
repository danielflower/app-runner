package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.AppManager;
import com.danielflower.apprunner.mgmt.FileBasedGitRepoLoader;
import com.danielflower.apprunner.mgmt.GitRepoLoader;
import com.danielflower.apprunner.runners.*;
import com.danielflower.apprunner.web.ProxyMap;
import com.danielflower.apprunner.web.WebServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.danielflower.apprunner.Config.SERVER_PORT;

public class App {
    public static final Logger log = LoggerFactory.getLogger(App.class);
    private final Config config;
    private WebServer webServer;
    private AppEstate estate;

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

    public App(Config config) {
        this.config = config;
    }

    public void start() throws Exception {
        File dataDir = config.getOrCreateDir(Config.DATA_DIR);
        FileSandbox fileSandbox = new FileSandbox(dataDir);
        GitRepoLoader gitRepoLoader = FileBasedGitRepoLoader.getGitRepoLoader(dataDir);
        addSampleAppIfNoAppsAlreadyThere(gitRepoLoader, config);

        ProxyMap proxyMap = new ProxyMap();

        int appRunnerPort = config.getInt(SERVER_PORT);
        URI appRunnerInternalUrl = URI.create("http://localhost:" + appRunnerPort);

        RunnerProvider runnerProvider = createRunnerProvider(config, fileSandbox);
        estate = new AppEstate(appRunnerInternalUrl, proxyMap, fileSandbox, runnerProvider);


        for (Map.Entry<String, String> repo : gitRepoLoader.loadAll().entrySet()) {
            estate.addApp(repo.getValue(), repo.getKey());
        }
        estate.addAppAddedListener(app -> gitRepoLoader.save(app.name(), app.gitUrl()));

        estate.all().forEach(a -> {
            StringBuilderWriter writer = new StringBuilderWriter();
            try {
                estate.update(a.name(), new OutputToWriterBridge(writer));
            } catch (Exception e) {
                log.warn("Error while starting up " + a.name() + "\nLogs:\n" + writer, e);
            }
        });

        String defaultAppName = config.get(Config.DEFAULT_APP_NAME, null);
        webServer = new WebServer(appRunnerPort, proxyMap, estate, defaultAppName);
        webServer.start();
    }

    public static RunnerProvider createRunnerProvider(Config config, FileSandbox fileSandbox) {
        List<AppRunner.Factory> runnerFactories = new ArrayList<>();

        if (config.hasItem("JAVA_HOME")) {

            File javaHome = config.getDir("JAVA_HOME");

            if (config.hasItem("LEIN_JAR")) {
                File leinJavaExecutable = config.hasItem("LEIN_JAVA_CMD")
                    ? config.getFile("LEIN_JAVA_CMD")
                    : FileUtils.getFile(javaHome, "bin", SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java");
                runnerFactories.add(new LeinRunner.Factory(
                    config.getFile("LEIN_JAR"), leinJavaExecutable, fileSandbox));
            }

            runnerFactories.add(new MavenRunner.Factory(javaHome));
        }


        for (AppRunner.Factory runnerFactory : runnerFactories) {
            log.info("Registered " + runnerFactory);
        }
        return new RunnerProvider(runnerFactories);
    }

    public static void addSampleAppIfNoAppsAlreadyThere(GitRepoLoader gitRepoLoader, Config config) throws Exception {
        if (gitRepoLoader.loadAll().size() == 0) {
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
        }
    }

}
