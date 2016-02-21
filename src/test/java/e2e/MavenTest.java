package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.util.Fields;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.AppRepo;

import java.io.File;
import java.util.HashMap;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;
import static scaffolding.TestConfig.config;

public class MavenTest {

    final String port = "48183";
    final String appRunnerUrl = "http://localhost:" + port;
    final String appResourceUri = appRunnerUrl + "/api/v1/apps";

    final App app = new App(new Config(new HashMap<String,String>() {{
        put(Config.SERVER_PORT, port);
        put(Config.DATA_DIR, dirPath(new File("target/datadirs/" + System.currentTimeMillis())));
        put("JAVA_HOME", dirPath(config.javaHome()));
    }}));

    final HttpClient client = new HttpClient();
    final String appId = "maven";

    AppRepo appRepo;

    @Before public void start() throws Exception {
        client.start();

        appRepo = AppRepo.create(appId);
        app.start();

        ContentResponse created = client.POST(appResourceUri)
            .content(new FormContentProvider(new Fields() {{
                add("gitUrl", appRepo.gitUrl());
            }}))
            .send();

        assertThat(created.getStatus(), is(201));

        ContentResponse update = client.POST(appResourceUri + "/" + appId + "/deploy").send();
        assertThat(update.getStatus(), is(200));
    }

    @After public void stopClient() throws Exception {
        client.stop();
    }

    @After public void shutdownApp() {
        app.shutdown();
    }

    @Test
    public void canCloneARepoAndStartItAndRestartingAppRunnerIsFine() throws Exception {
        assertThat(
            client.GET(appRunnerUrl + "/" + appId + "/"),
            is(equalTo(200, containsString("My Maven App"))));

//        app.shutdown();
//        app = new App(new Config(config));
//        app.start();
//
//        resp = client.GET(appRunnerUrl + "/maven/");
//        assertThat(resp.getStatus(), is(200));
//        assertThat(resp.getContentAsString(), containsString("My Maven App"));

        File indexHtml = new File(appRepo.originDir, FilenameUtils.separatorsToSystem("src/main/resources/web/index.html"));
        String newVersion = FileUtils.readFileToString(indexHtml).replace("My Maven App", "My new and improved maven app!");
        FileUtils.write(indexHtml, newVersion, false);
        appRepo.origin.add().addFilepattern(".").call();
        appRepo.origin.commit().setMessage("Updated index.html").setAuthor("Dan F", "danf@example.org").call();

        assertThat(
            client.POST(appRunnerUrl + "/api/v1/apps/" + appId + "/deploy").send(),
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

        assertThat(
            client.POST(appRunnerUrl + "/api/v1/apps/invalid-app-name/deploy").send(),
            is(equalTo(404, is("No app found with name 'invalid-app-name'. Valid names: maven"))));

        resp = client.GET(appRunnerUrl + "/api/v1/apps/maven");
        assertThat(resp.getStatus(), is(200));
        JSONObject single = new JSONObject(resp.getContentAsString());
        JSONAssert.assertEquals(all.getJSONArray("apps").getJSONObject(0), single, JSONCompareMode.STRICT_ORDER);
    }
}
