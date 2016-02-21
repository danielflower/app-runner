package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.util.Fields;
import org.junit.*;
import scaffolding.AppRepo;
import scaffolding.Dirs;

import java.io.File;
import java.util.HashMap;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;

public class ClojureTest {

    final String port = "48183";
    final String appRunnerUrl = "http://localhost:" + port;
    final String appResourceUri = appRunnerUrl + "/api/v1/apps";

    final App app = new App(new Config(new HashMap() {{
        put(Config.SERVER_PORT, port);
        put(Config.DATA_DIR, dirPath(new File("target/datadirs/" + System.currentTimeMillis())));
        put("JAVA_HOME", dirPath(Dirs.javaHome));
        put("LEIN_JAR", dirPath(Dirs.leinJar.get()));
    }}));

    final HttpClient client = new HttpClient();
    final String appId = "lein";

    AppRepo appRepo;

    @BeforeClass
    public static void setup() throws Exception {
        Assume.assumeTrue("Skipping tests as LEIN not detected", Dirs.leinJar.isPresent());
    }

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
            is(equalTo(200, containsString("Hello from lein"))));

    }

}
