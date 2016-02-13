package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.problems.AppRunnerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.AppRepo;
import scaffolding.Photocopier;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
    private HashMap<String, String> config;

    @Before
    public void start() throws Exception {
        client = new HttpClient();
        client.start();
        String port = "48183";
        appRunnerUrl = "http://localhost:" + port;
        appRepo = AppRepo.create("maven");

        File gitRepoFile = Photocopier.tempFile("gitrepos.txt");
        FileUtils.writeLines(gitRepoFile, asList(appRepo.gitUrl()));

        config = new HashMap<>();
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
        app = new App(new Config(config));
        app.start();

        resp = client.GET(appRunnerUrl + "/maven/");
        assertThat(resp.getStatus(), is(200));
        assertThat(resp.getContentAsString(), containsString("My Maven App"));

        File indexHtml = new File(appRepo.originDir, FilenameUtils.separatorsToSystem("src/main/resources/web/index.html"));
        String newVersion = FileUtils.readFileToString(indexHtml).replace("My Maven App", "My new and improved maven app!");
        FileUtils.write(indexHtml, newVersion, false);
        appRepo.origin.add().addFilepattern(".").call();
        appRepo.origin.commit().setMessage("Updated index.html").setAuthor("Dan F", "danf@example.org").call();

        System.out.println("Sending request");
        resp = client.newRequest(appRunnerUrl + "/api/v1/apps/maven")
            .method("PUT")
            .send();
        assertThat(resp.getStatus(), is(200));
//        assertThat(resp.getContentAsString(), is("Blah blah"));
        System.out.println("Response consumed");

        resp = client.GET(appRunnerUrl + "/maven/");
        assertThat(resp.getStatus(), is(200));
        assertThat(resp.getContentAsString(), containsString("My new and improved maven app!"));
    }

    @Test
    public void theRestAPILives() throws Exception {
        ContentResponse resp = client.GET(appRunnerUrl + "/api/v1/apps");
        assertThat(resp.getStatus(), is(200));
        String json = resp.getContentAsString();
        JSONAssert.assertEquals("{apps:[ {name: \"maven\"} ]}", json, JSONCompareMode.LENIENT);

        resp = client.newRequest(appRunnerUrl + "/api/v1/apps/invalid-app-name")
            .method("PUT")
            .send();
        assertThat(resp.getStatus(), is(404));
        assertThat(resp.getContentAsString(), is("No app found with name 'invalid-app-name'. Valid names: maven"));
    }

}
