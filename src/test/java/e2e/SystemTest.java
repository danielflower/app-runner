package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.AppRepo;
import scaffolding.RestClient;

import java.io.File;
import java.util.HashMap;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;
import static scaffolding.TestConfig.config;

public class SystemTest {

    final String port = "48183";
    final String appRunnerUrl = "http://localhost:" + port;
    final RestClient restClient = RestClient.create(appRunnerUrl);
    final HttpClient client = new HttpClient();
    final String appId = "maven";
    final AppRepo appRepo = AppRepo.create(appId);
    final File dataDir = new File("target/datadirs/" + System.currentTimeMillis());
    App app = startedApp();

    private App startedApp() {
        App app = new App(new Config(new HashMap<String, String>() {{
            put(Config.SERVER_PORT, port);
            put(Config.DATA_DIR, dirPath(dataDir));
            put("JAVA_HOME", dirPath(config.javaHome()));
        }}));
        try {
            app.start();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't start test app", e);
        }
        return app;
    }


    @Before public void start() throws Exception {
        client.start();
        assertThat(restClient.createApp(appRepo.gitUrl()).getStatus(), is(201));
        assertThat(restClient.deploy(appId).getStatus(), is(200));
    }

    @After public void stopClient() throws Exception {
        restClient.stop();
        client.stop();
    }

    @After public void shutdownApp() {
        app.shutdown();
    }

    @Test
    public void canCloneARepoAndStartItAndRestartingAppRunnerIsFine() throws Exception {
        assertThat(restClient.homepage(appId), is(equalTo(200, containsString("My Maven App"))));

        app.shutdown();
        app = startedApp();

        assertThat(restClient.homepage(appId), is(equalTo(200, containsString("My Maven App"))));

        File indexHtml = new File(appRepo.originDir, FilenameUtils.separatorsToSystem("src/main/resources/web/index.html"));
        String newVersion = FileUtils.readFileToString(indexHtml).replace("My Maven App", "My new and improved maven app!");
        FileUtils.write(indexHtml, newVersion, false);
        appRepo.origin.add().addFilepattern(".").call();
        appRepo.origin.commit().setMessage("Updated index.html").setAuthor("Dan F", "danf@example.org").call();

        assertThat(restClient.deploy(appId),
            is(equalTo(200, allOf(
                containsString("Going to build and deploy " + appId),
                containsString("Success")))));

        JSONObject appInfo = new JSONObject(client.GET(appRunnerUrl + "/api/v1/apps/" + appId).getContentAsString());

        assertThat(
            client.GET(appInfo.getString("url")),
            is(equalTo(200, containsString("My new and improved maven app!"))));

        assertThat(
            client.GET(appInfo.getString("buildLogUrl")),
            is(equalTo(200, containsString("[INFO] Building my-maven-app 1.0-SNAPSHOT"))));
        assertThat(
            client.GET(appInfo.getString("consoleLogUrl")),
            is(equalTo(200, containsString("Starting maven in prod"))));
    }

    @Test
    public void theRestAPILives() throws Exception {
        ContentResponse resp = client.GET(appRunnerUrl + "/api/v1/apps");
        assertThat(resp.getStatus(), is(200));

        JSONObject all = new JSONObject(resp.getContentAsString());

        JSONAssert.assertEquals("{apps:[ {" +
            "name: \"maven\"," +
            "url: \"" + appRunnerUrl + "/maven/\"" +
            "} ]}", all, JSONCompareMode.LENIENT);

        assertThat(restClient.deploy("invalid-app-name"),
            is(equalTo(404, is("No app found with name 'invalid-app-name'. Valid names: maven"))));

        resp = client.GET(appRunnerUrl + "/api/v1/apps/maven");
        assertThat(resp.getStatus(), is(200));
        JSONObject single = new JSONObject(resp.getContentAsString());
        JSONAssert.assertEquals(all.getJSONArray("apps").getJSONObject(0), single, JSONCompareMode.STRICT_ORDER);
    }
}
