package com.danielflower.apprunner;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import com.danielflower.apprunner.mgmt.AppManager;
import com.danielflower.apprunner.mgmt.FileBasedGitRepoLoader;
import com.danielflower.apprunner.mgmt.GitRepoLoader;
import com.danielflower.apprunner.runners.*;
import com.danielflower.apprunner.web.ProxyMap;
import com.danielflower.apprunner.web.WebServer;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.danielflower.apprunner.Config.SERVER_PORT;

public class App {
    public static final Logger log = LoggerFactory.getLogger(App.class);
    private final Config config;
    private WebServer webServer;
    private AppEstate estate;

    public App(Config config) {
        this.config = config;
    }

    public void start() throws Exception {
        File dataDir = config.getOrCreateDir(Config.DATA_DIR);
        GitRepoLoader gitRepoLoader = FileBasedGitRepoLoader.getGitRepoLoader(dataDir);
        addSampleAppIfNoAppsAlreadyThere(gitRepoLoader);

        ProxyMap proxyMap = new ProxyMap();

        int appRunnerPort = config.getInt(SERVER_PORT);
        FileSandbox fileSandbox = new FileSandbox(dataDir);

        estate = new AppEstate(
            proxyMap,
            fileSandbox,
            createRunnerProvider(fileSandbox));

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

    private RunnerProvider createRunnerProvider(FileSandbox fileSandbox) {
        List<AppRunner.Factory> runnerFactories = new ArrayList<>();

        config.leinJar().ifPresent(lj -> runnerFactories.add(new LeinRunner.Factory(lj, config.leinJavaCommandProvider(), fileSandbox)));

        runnerFactories.add(new MavenRunner.Factory(config.javaHomeProvider()));

        config.nodeExecutable().ifPresent(node -> runnerFactories.add(new NodeRunner.Factory(node, config.npmExecutable().get())));

        runnerFactories.stream().forEach( f -> log.info("Registered " + f));

        return new RunnerProvider(runnerFactories);
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
