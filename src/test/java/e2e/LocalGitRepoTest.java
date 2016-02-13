package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.problems.AppRunnerException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import scaffolding.AppRepo;
import scaffolding.Photocopier;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LocalGitRepoTest {

    private App app;
    private AppRepo appRepo;
    private String appRunnerUrl;
    private HttpClient client;

    @Before
    public void start() throws Exception {
        client = new HttpClient();
        client.start();
        String port = "48183";
        appRunnerUrl = "http://localhost:" + port;
        appRepo = AppRepo.create("maven");

        File gitRepoFile = Photocopier.tempFile("gitrepos.txt");
        FileUtils.writeLines(gitRepoFile, asList(appRepo.gitUrl()));

        Map<String, String> config = new HashMap<>();
        config.put(Config.SERVER_PORT, port);
        config.put(Config.DATA_DIR, dirPath(new File("target/datadirs/" + System.currentTimeMillis())));
        config.put(Config.REPO_FILE_PATH, dirPath(gitRepoFile));
        app = new App(new Config(config));
        app.start();

    }

    @After
    public void stop() {
        try {
            client.stop();
        } catch (Exception e) {
            throw new AppRunnerException(e);
        }
        app.shutdown();
    }

    @Test
    public void canCloneARepoAndStartItAndRestartingAppRunnerIsFine() throws Exception {
        ContentResponse resp = client.GET(appRunnerUrl + "/maven/");
        assertThat(resp.getStatus(), is(200));
        assertThat(resp.getContentAsString(), containsString("My Maven App"));

        app.shutdown();
        app.start();

        resp = client.GET(appRunnerUrl + "/maven/");
        assertThat(resp.getStatus(), is(200));
        assertThat(resp.getContentAsString(), containsString("My Maven App"));
    }

}
