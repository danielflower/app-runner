package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.util.Fields;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.AppRepo;
import scaffolding.Dirs;

import java.io.File;
import java.util.HashMap;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;

public class LocalGitRepoTest {

    final String port = "48183";
    final String appRunnerUrl = "http://localhost:" + port;
    final String appResourceUri = appRunnerUrl + "/api/v1/apps";

    final App app = new App(new Config(new HashMap() {{
        put(Config.SERVER_PORT, port);
        put(Config.DATA_DIR, dirPath(new File("target/datadirs/" + System.currentTimeMillis())));
        put("JAVA_HOME", dirPath(Dirs.javaHome));
    }}));

    final HttpClient client = new HttpClient();

    AppRepo appRepo;

    @Before public void start() throws Exception {
        client.start();

        appRepo = AppRepo.create("maven");
        app.start();

        ContentResponse created = client.POST(appResourceUri)
            .content(new FormContentProvider(new Fields() {{
                add("gitUrl", appRepo.gitUrl());
            }}))
            .send();

        assertThat(created.getStatus(), is(201));

        ContentResponse update = client.POST(appResourceUri + "/maven").send();
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
            client.GET(appRunnerUrl + "/maven/"),
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
            client.POST(appRunnerUrl + "/api/v1/apps/maven").send(),
            is(equalTo(200, allOf(
                containsString("Going to build and deploy maven"),
                containsString("Success")))));

        assertThat(
            client.GET(appRunnerUrl + "/maven/"),
            is(equalTo(200, containsString("My new and improved maven app!"))));
    }

    @Test
    public void theRestAPILives() throws Exception {
        ContentResponse resp = client.GET(appRunnerUrl + "/api/v1/apps");
        assertThat(resp.getStatus(), is(200));

        String json = resp.getContentAsString();
        JSONAssert.assertEquals("{apps:[ {name: \"maven\"} ]}", json, JSONCompareMode.LENIENT);

        assertThat(
            client.POST(appRunnerUrl + "/api/v1/apps/invalid-app-name").send(),
            is(equalTo(404, is("No app found with name 'invalid-app-name'. Valid names: maven"))));
    }
}
