package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import org.junit.*;
import scaffolding.AppRepo;
import scaffolding.RestClient;

import java.io.File;
import java.util.HashMap;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;
import static scaffolding.TestConfig.config;

public class ClojureTest {

    @BeforeClass
    public static void skipIfUnsupported() throws Exception {
        Assume.assumeTrue("Skipping tests as LEIN not detected", config.leinJar().isPresent());
    }

    final String port = "48183";
    final String appRunnerUrl = "http://localhost:" + port;
    final RestClient restClient = RestClient.create(appRunnerUrl);
    final String appId = "lein";
    final AppRepo appRepo = AppRepo.create(appId);

    final App app = new App(new Config(new HashMap<String,String>() {{
        put(Config.SERVER_PORT, port);
        put(Config.DATA_DIR, dirPath(new File("target/datadirs/" + System.currentTimeMillis())));
        put("JAVA_HOME", dirPath(config.javaHome()));
        put("LEIN_JAR", dirPath(config.leinJar().get()));
    }}));


    @Before public void start() throws Exception {
        app.start();
    }

    @After public void shutdownApp() {
        restClient.stop();
        app.shutdown();
    }

    @Test
    public void canCreateAnAppViaTheRestAPI() throws Exception {
        assertThat(restClient.createApp(appRepo.gitUrl()).getStatus(), is(201));
        assertThat(restClient.deploy(appId).getStatus(), is(200));
        assertThat(restClient.homepage(appId), is(equalTo(200, containsString("Hello from lein"))));
    }

}
