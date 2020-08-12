package com.danielflower.apprunner;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import com.danielflower.apprunner.mgmt.*;
import com.danielflower.apprunner.runners.AppRunnerFactoryProvider;
import com.danielflower.apprunner.runners.UnsupportedProjectTypeException;
import com.danielflower.apprunner.web.ProxyMap;
import com.danielflower.apprunner.web.WebServer;
import com.danielflower.apprunner.web.v1.AppResource;
import com.danielflower.apprunner.web.v1.SystemResource;
import io.muserver.HttpsConfigBuilder;
import io.muserver.acme.AcmeCertManager;
import io.muserver.acme.AcmeCertManagerBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

public class App {
    public static final Logger log = LoggerFactory.getLogger(App.class);

    private final Config config;
    private WebServer webServer;
    private AppEstate estate;
    private final AtomicBoolean startupComplete = new AtomicBoolean(false);
    private BackupService backupService;

    public App(Config config) {
        this.config = config;
    }

    public void start() throws Exception {
        SystemInfo systemInfo = SystemInfo.create();
        log.info(systemInfo.toString());

        File dataDir = config.getOrCreateDir(Config.DATA_DIR);
        FileSandbox fileSandbox = new FileSandbox(dataDir);

        deleteOldTempFiles(fileSandbox.tempDir(""));

        GitRepoLoader gitRepoLoader = FileBasedGitRepoLoader.getGitRepoLoader(dataDir);
        addSampleAppIfNoAppsAlreadyThere(gitRepoLoader);

        ProxyMap proxyMap = new ProxyMap();

        log.info("Detecting providers...");
        AppRunnerFactoryProvider runnerProvider = AppRunnerFactoryProvider.create(config);
        log.info("Registered providers..." + LINE_SEPARATOR + runnerProvider.describeRunners());

        estate = new AppEstate(
            proxyMap,
            fileSandbox,
            runnerProvider);

        for (Map.Entry<String, String> repo : gitRepoLoader.loadAll().entrySet()) {
            try {
                estate.addApp(repo.getValue(), repo.getKey());
            } catch (UnsupportedProjectTypeException | GitAPIException e) {
                log.warn("Error while trying to initiliase " + repo.getKey() + " (" + repo.getValue() + ") - will ignore this app.", e);
            }
        }

        estate.addAppAddedListener(app -> gitRepoLoader.save(app.name(), app.gitUrl()));
        estate.addAppDeletedListener(app -> gitRepoLoader.delete(app.name()));

        int redirectToHttps = Integer.parseInt(config.get("apprunner.redirecttohttps.port", "-1"));

        String defaultAppName = config.get(Config.DEFAULT_APP_NAME, null);

        int httpPort = config.getInt(Config.SERVER_HTTP_PORT, -1);
        int httpsPort = config.getInt(Config.SERVER_HTTPS_PORT, -1);
        HttpsConfigBuilder sslContext = null;
        AcmeCertManager acmeCertManager = null;
        if (httpsPort > -1) {
            String acmeUri = config.get("apprunner.acme.serverUri", null);
            if (acmeUri != null) {
                String[] domains = config.get("apprunner.acme.domain").split(",");
                AcmeCertManagerBuilder builder = AcmeCertManagerBuilder.acmeCertManager()
                    .withAcmeServerURI(URI.create(acmeUri))
                    .withConfigDir(config.get("apprunner.acme.configDir"));
                for (String domain : domains) {
                    builder.withDomain(domain.trim());
                }
                acmeCertManager = builder.build();
            } else {
                sslContext = HttpsConfigBuilder.httpsConfig()
                    .withKeystore(config.getFile("apprunner.keystore.path"))
                    .withKeystorePassword(config.get("apprunner.keystore.password"))
                    .withKeyPassword(config.get("apprunner.keymanager.password"))
                    .withKeystoreType(config.get("apprunner.keystore.type", "JKS"));
            }
        }

        String backupUrl = config.get(Config.BACKUP_URL, "");
        if (StringUtils.isNotBlank(backupUrl)) {
            backupService = BackupService.prepare(dataDir, new URIish(backupUrl), config.getInt(Config.BACKUP_MINUTES, 60));
            backupService.start();
        }

        String viaName = config.get("apprunner.proxy.via.name", "apprunner");


        int idleTimeout = config.getInt("apprunner.proxy.idle.timeout", 30000);
        int totalTimeout = config.getInt("apprunner.proxy.total.timeout", 60000);


        AppResource appResource = new AppResource(estate, systemInfo, fileSandbox);
        SystemResource systemResource = new SystemResource(systemInfo, startupComplete, runnerProvider.factories(), backupService);

        long maxRequestSize = config.getLong("apprunner.request.max.size.bytes", 500 * 1024 * 1024L);
        webServer = new WebServer(httpPort, httpsPort, sslContext, acmeCertManager, redirectToHttps, proxyMap, defaultAppName,
            systemResource, appResource, idleTimeout, totalTimeout, viaName, maxRequestSize);

        webServer.start();

        if (httpsPort >= -0) {
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            if (maxKeyLen < 8192) {
                log.warn("The current java version (" + System.getProperty("java.home") + ") limits key length to " + maxKeyLen + " bits so modern browsers may have issues connecting. Upgrade to the latest JDK to allow high strength SSL connections.");
            }
        }
        deployAllAppsAsyncronously(estate, defaultAppName);
    }

    private void deployAllAppsAsyncronously(AppEstate estate, String firstAppToStart) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        estate.appsByStartupOrder(firstAppToStart)
            .forEach(a -> executor.submit(() -> {
                try (StringBuilderWriter writer = new StringBuilderWriter()) {
                    try {
                        estate.update(a.name(), new OutputToWriterBridge(writer));
                    } catch (Exception e) {
                        log.warn("Error while starting up " + a.name() + LINE_SEPARATOR + "Logs:" + LINE_SEPARATOR + writer, e);
                    }
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
        long start = System.currentTimeMillis();
        log.info("Deleting contents of temporary folder at " + fullPath(tempDir));
        try {
            FileUtils.deleteDirectory(tempDir);
            log.info("File deletion complete in " + (System.currentTimeMillis() - start) + "ms");
        } catch (IOException e) {
            log.warn("Failed to delete " + fullPath(tempDir), e);
        }
    }

    private void addSampleAppIfNoAppsAlreadyThere(GitRepoLoader gitRepoLoader) throws Exception {
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

        if (backupService != null) {
            log.info("Shutting down backup service");
            try {
                backupService.stop();
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
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
