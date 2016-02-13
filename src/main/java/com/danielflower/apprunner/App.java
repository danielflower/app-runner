package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.AppManager;
import com.danielflower.apprunner.mgmt.FileBasedGitRepoLoader;
import com.danielflower.apprunner.mgmt.GitRepoLoader;
import com.danielflower.apprunner.problems.InvalidConfigException;
import com.danielflower.apprunner.web.ProxyMap;
import com.danielflower.apprunner.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import static com.danielflower.apprunner.Config.SERVER_PORT;
import static com.danielflower.apprunner.FileSandbox.dirPath;

public class App {
    public static final Logger log = LoggerFactory.getLogger(App.class);
    private final Config config;
    private WebServer webServer;
    private final AppEstate estate = new AppEstate();

    public static void main(String[] args) {
        try {
            App app = new App(new Config(System.getenv()));
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
        FileSandbox fileSandbox = new FileSandbox(config.getDir(Config.DATA_DIR));
        GitRepoLoader gitRepoLoader = getGitRepoLoader(config);
        ProxyMap proxyMap = new ProxyMap();
        for (String repo : gitRepoLoader.loadAll()) {
            AppManager appMan = AppManager.create(repo, fileSandbox);
            appMan.addListener(proxyMap::add);
            estate.add(appMan);
            appMan.update(new Slf4jWriterBridge());
        }
        webServer = new WebServer(config.getInt(SERVER_PORT), proxyMap, estate);
        webServer.start();
    }

    private static GitRepoLoader getGitRepoLoader(Config config) {
        GitRepoLoader gitRepoLoader = null;
        String gitRepoProps = config.get(Config.REPO_FILE_PATH, null);
        if (gitRepoProps != null) {
            File repoFile = new File(gitRepoProps);
            log.info("Using file-based git provider: " + dirPath(repoFile));
            gitRepoLoader = new FileBasedGitRepoLoader(repoFile);
        }
        if (gitRepoLoader == null) {
            throw new InvalidConfigException("There is no git repo source. Please set " + Config.REPO_FILE_PATH + " or " + Config.STASH_PROJECT_URL);
        }
        return gitRepoLoader;
    }

    public void shutdown() {
        log.info("Shutdown invoked");
        if (webServer != null) {
            try {
                webServer.close();
            } catch (Exception e) {
                log.info("Error while stopping", e);
            }
            estate.shutdown();
            log.info("Shutdown complete");
        }
    }

    private static class Slf4jWriterBridge extends Writer {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            log.info("Update output: " + new String(cbuf, off, len));
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }
}
