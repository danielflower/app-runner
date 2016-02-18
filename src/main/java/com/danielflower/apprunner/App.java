package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.FileBasedGitRepoLoader;
import com.danielflower.apprunner.mgmt.GitRepoLoader;
import com.danielflower.apprunner.runners.OutputToWriterBridge;
import com.danielflower.apprunner.web.ProxyMap;
import com.danielflower.apprunner.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
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
        ProxyMap proxyMap = new ProxyMap();

        estate = new AppEstate(proxyMap, fileSandbox, config.getDir("JAVA_HOME"));
        for (Map.Entry<String, String> repo : gitRepoLoader.loadAll().entrySet()) {
            estate.addApp(repo.getValue(), repo.getKey());
        }
        estate.addAppAddedListener(app -> gitRepoLoader.save(app.name(), app.gitUrl()));

        estate.all().forEach(a -> {
            try {
                estate.update(a.name(), new OutputToWriterBridge(new NullWriter()));
            } catch (Exception e) {
                log.warn("Error while starting up " + a.name(), e);
            }
        });

        String defaultAppName = config.get(Config.DEFAULT_APP_NAME, null);
        webServer = new WebServer(config.getInt(SERVER_PORT), proxyMap, estate, defaultAppName);
        webServer.start();
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

    private static class NullWriter extends Writer {
        public void write(char[] cbuf, int off, int len) throws IOException { }
        public void flush() throws IOException { }
        public void close() throws IOException { }
    }
}
