package e2e;

import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.runners.JavaHomeProvider;
import com.danielflower.apprunner.runners.MavenRunner;
import com.danielflower.apprunner.runners.Waiter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.json.JSONObject;
import org.junit.*;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.AppRepo;
import scaffolding.RestClient;
import scaffolding.TestConfig;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;

public class SystemTest {

    static final String port = "48183";
    static final String appRunnerUrl = "http://localhost:" + port;
    static final RestClient restClient = RestClient.create(appRunnerUrl);
    static final HttpClient client = new HttpClient();
    static final String appName = "maven";
    static final AppRepo appRepo = AppRepo.create(appName);
    static final File dataDir = new File("target/datadirs/" + System.currentTimeMillis());
    static MavenRunner mavenRunner;


    @BeforeClass
    public static void setup() throws Exception {
        buildAndStartUberJar();
        client.start();
        assertThat(restClient.createApp(appRepo.gitUrl()).getStatus(), is(201));
        assertThat(restClient.deploy(appName).getStatus(), is(200));
    }

    public static void buildAndStartUberJar() throws Exception {
        mavenRunner = new MavenRunner(new File("."), JavaHomeProvider.default_java_home,
            asList("-DskipTests=true", "package"));
        Map<String, String> env = new HashMap<String, String>(System.getenv()) {{
            put(Config.SERVER_PORT, port);
            put(Config.DATA_DIR, dirPath(dataDir));
            put("JAVA_HOME", dirPath(TestConfig.config.javaHome()));
        }};
        InvocationOutputHandler logHandler = line -> System.out.println("Test build output > " + line);
        URI appRunnerURL = URI.create(appRunnerUrl + "/");
        try (Waiter startupWaiter = Waiter.waitFor("AppRunner uber jar", appRunnerURL, 2, TimeUnit.MINUTES)) {
            mavenRunner.start(logHandler, logHandler, env, startupWaiter);
        }
    }

    public static void shutDownAppRunner() throws Exception {
        restClient.stop(appName);
        mavenRunner.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        shutDownAppRunner();
        restClient.stop();
        client.stop();
    }


    @Test
    public void canCloneARepoAndStartItAndRestartingAppRunnerIsFine() throws Exception {
        assertThat(restClient.homepage(appName), is(equalTo(200, containsString("My Maven App"))));

        shutDownAppRunner();
        buildAndStartUberJar();

        assertThat(restClient.homepage(appName), is(equalTo(200, containsString("My Maven App"))));

        File indexHtml = new File(appRepo.originDir, FilenameUtils.separatorsToSystem("src/main/resources/web/index.html"));
        String newVersion = FileUtils.readFileToString(indexHtml).replace("My Maven App", "My new and improved maven app!");
        FileUtils.write(indexHtml, newVersion, false);
        appRepo.origin.add().addFilepattern(".").call();
        appRepo.origin.commit().setMessage("Updated index.html").setAuthor("Dan F", "danf@example.org").call();

        assertThat(restClient.deploy(appName),
            is(equalTo(200, allOf(
                containsString("Going to build and deploy " + appName),
                containsString("Success")))));

        JSONObject appInfo = new JSONObject(client.GET(appRunnerUrl + "/api/v1/apps/" + appName).getContentAsString());

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
