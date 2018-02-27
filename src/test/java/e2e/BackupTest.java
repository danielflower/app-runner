package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.web.WebServer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import scaffolding.AppRepo;
import scaffolding.Photocopier;
import scaffolding.RestClient;

import java.io.File;
import java.util.HashMap;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.FileExistsMatcher.fileExists;

public class BackupTest {

    private final String port = String.valueOf(WebServer.getAFreePort());
    private final String appRunnerUrl = "http://localhost:" + port;
    private final RestClient restClient = RestClient.create(appRunnerUrl);
    private final String appId = "maven";
    private final AppRepo appRepo = AppRepo.create(appId);
    private App app;
    private URIish backupUri;

    @Before public void start() throws Exception {
        File backupDir = Photocopier.folderForSampleProject("backup");
        Git.init().setDirectory(backupDir).setBare(true).call();
        backupUri = new URIish(backupDir.toURI().toURL());
        app = new App(new Config(new HashMap<String,String>() {{
            put(Config.SERVER_HTTP_PORT, port);
            put(Config.BACKUP_URL, backupUri.toString());
            put(Config.DATA_DIR, fullPath(new File("target/datadirs/" + System.currentTimeMillis())));
        }}));
        System.out.println("************************************");
        app.start();
        System.out.println("************************************");
    }

    @After public void shutdownApp() {
        app.shutdown();
    }

    @Test
    public void theDataDirCanBeBackedUpToARemoteGitRepo() throws Exception {
        restClient.createApp(appRepo.gitUrl());

        // restart to guarantee at least one backup
        app.shutdown();
        app.start();

        File backupCopyDir = Photocopier.folderForSampleProject("clone");
        Git.cloneRepository().setDirectory(backupCopyDir).setBare(false).setURI(backupUri.toString()).call();
        assertThat(new File(backupCopyDir, "repos.properties"), fileExists());

    }

}
