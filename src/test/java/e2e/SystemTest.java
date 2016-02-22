package e2e;

import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.runners.JavaHomeProvider;
import com.danielflower.apprunner.runners.MavenRunner;
import com.danielflower.apprunner.web.AppResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.SystemOutHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.util.Fields;
import org.json.JSONObject;
import org.junit.*;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.AppRepo;
import scaffolding.RestClient;
import scaffolding.TestConfig;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;

public class SystemTest {

    static final String port = "48183";
    static final String appRunnerUrl = "http://localhost:" + port;
    final RestClient restClient = RestClient.create(appRunnerUrl);
    static final HttpClient client = new HttpClient();
    final String appId = "maven";
    final AppRepo appRepo = AppRepo.create(appId);
    static final File dataDir = new File("target/datadirs/" + System.currentTimeMillis());
    static MavenRunner mavenRunner;


    @BeforeClass
    public static void buildAndStartUberJar() throws MavenInvocationException {
        mavenRunner = new MavenRunner(new File("."), JavaHomeProvider.default_java_home,
            asList("-DskipTests=true", "package"));
        Map<String, String> env = new HashMap<String, String>(System.getenv()) {{
            put(Config.SERVER_PORT, port);
            put(Config.DATA_DIR, dirPath(dataDir));
            put("JAVA_HOME", dirPath(TestConfig.config.javaHome()));
        }};
        InvocationOutputHandler logHandler = line -> System.out.println("Test build output > " + line);
        mavenRunner.start(logHandler, logHandler, env);
    }

    @AfterClass
    public static void shutDownAppRunner() throws InterruptedException, ExecutionException, TimeoutException {
        try {
            client.POST(appRunnerUrl + "/api/v1/apps/die")
                .content(new FormContentProvider(new Fields() {{
                    add("password", AppResource.deathPassword);
                }})).send();
        } catch (ExecutionException e) {
            // this is expected... it dies before the execution is complete
        }
        mavenRunner.shutdown();
    }

    static boolean alreadyCreated = false;

    @Before
    public void start() throws Exception {
        client.start();
        if (!alreadyCreated) {
            assertThat(restClient.createApp(appRepo.gitUrl()).getStatus(), is(201));
            assertThat(restClient.deploy(appId).getStatus(), is(200));
            alreadyCreated = true;
            // This is so offensive and terrible. I'm sorry.
        }
    }

    @After
    public void stopClient() throws Exception {
        restClient.stop();
        client.stop();
    }

    @Test
    public void canCloneARepoAndStartItAndRestartingAppRunnerIsFine() throws Exception {
        assertThat(restClient.homepage(appId), is(equalTo(200, containsString("My Maven App"))));

        shutDownAppRunner();
        buildAndStartUberJar();
        while (true) {
            try {
                restClient.homepage(appId);
                break;
            } catch (ExecutionException e) {
                Thread.sleep(500);
                // it's starting up
            }
        }
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
