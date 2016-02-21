package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.util.Fields;
import org.junit.*;
import scaffolding.AppRepo;

import java.io.File;
import java.util.HashMap;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;
import static scaffolding.TestConfig.config;

public class NodeTest {

    final String port = "48189";
    final String appRunnerUrl = "http://localhost:" + port;
    final String appResourceUri = appRunnerUrl + "/api/v1/apps";

    final App app = new App(new Config(new HashMap<String,String>() {{
        put(Config.SERVER_PORT, port);
        put(Config.DATA_DIR, dirPath(new File("target/datadirs/" + System.currentTimeMillis())));
        put("NODE_HOME", dirPath(config.nodeExecutable().get().getParentFile()));
    }}));

    final HttpClient client = new HttpClient();
    final String appId = "nodejs";

    AppRepo appRepo;

    @BeforeClass
    public static void setup() throws Exception {
        Assume.assumeTrue("Skipping tests as LEIN not detected", config.leinJar().isPresent());
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
            is(equalTo(200, containsString("Hello from nodejs!"))));

    }

}
