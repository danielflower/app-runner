package e2e;

import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.mgmt.FileBasedGitRepoLoader;
import com.danielflower.apprunner.mgmt.GitRepoLoader;
import com.danielflower.apprunner.runners.*;
import com.danielflower.apprunner.web.WebServer;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.AppRepo;
import scaffolding.RestClient;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ContentResponseMatcher.equalTo;

public class SystemTest {

    private static final int httpsPort = WebServer.getAFreePort();
    private static final String appRunnerUrl = "https://localhost:" + httpsPort;
    private static final RestClient restClient = RestClient.create(appRunnerUrl);
    private static final AppRepo leinApp = AppRepo.create("lein");
    private static final AppRepo mavenApp = AppRepo.create("maven");
    private static final AppRepo nodeApp = AppRepo.create("nodejs");
    private static final File dataDir = new File("target/datadirs/" + System.currentTimeMillis());
    private static MavenRunner mavenRunner;
    private static final HttpClient client = RestClient.httpClient;

    @BeforeClass
    public static void setup() throws Exception {
        // ensure the zips exist
        new ZipSamplesTask().zipTheSamplesAndPutThemInTheResourcesDir();
        buildAndStartUberJar(asList("-DskipTests=true", "package"));

        createAndDeploy(mavenApp);
    }

    private static void createAndDeploy(AppRepo app) throws Exception {
        assertThat(restClient.createApp(app.gitUrl()).getStatus(), is(201));
        assertThat(restClient.deploy(app.name).getStatus(), is(200));
    }

    private static void buildAndStartUberJar(List<String> goals) throws Exception {
        mavenRunner = new MavenRunner(new File("."), new HomeProvider() {
            public InvocationRequest mungeMavenInvocationRequest(InvocationRequest request) {
                return HomeProvider.default_java_home.mungeMavenInvocationRequest(request);
            }

            public CommandLine commandLine(Map<String, String> envVarsForApp) {
                return HomeProvider.default_java_home.commandLine(envVarsForApp).addArgument("-Dlogback.configurationFile=src/test/resources/logback-test.xml");
            }
        }, goals);
        Map<String, String> env = new HashMap<String, String>(System.getenv()) {{
            put(Config.SERVER_HTTPS_PORT, String.valueOf(httpsPort));
            put("apprunner.keystore.path", fullPath(new File("local/test.keystore")));
            put("apprunner.keystore.password", "password");
            put("apprunner.keymanager.password", "password");
            put(Config.DATA_DIR, fullPath(dataDir));
        }};

        InvocationOutputHandler logHandler = line -> System.out.print("Uber jar output > " + line);
        try (Waiter startupWaiter = new Waiter("AppRunner uber jar", httpClient -> {
            try {
                JSONObject sysInfo = new JSONObject(client.GET(appRunnerUrl + "/api/v1/system").getContentAsString());
                return sysInfo.getBoolean("appRunnerStarted");
            } catch (Exception e) {
                return false;
            }
        }, 2, TimeUnit.MINUTES)) {
            mavenRunner.start(logHandler, logHandler, env, startupWaiter);
        }

    }

    private static void shutDownAppRunner() throws Exception {
        JSONObject apps = new JSONObject(restClient.get("/api/v1/apps").getContentAsString());
        for (Object o : apps.getJSONArray("apps")) {
            JSONObject app = (JSONObject) o;
            restClient.stop(app.getString("name"));
        }
        mavenRunner.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        shutDownAppRunner();
    }

    @Test
    public void leinAppsWork() throws Exception {
        LeinRunnerTest.ignoreTestIfNotSupported();
        createAndDeploy(leinApp);
        assertThat(restClient.homepage(leinApp.name),
            is(equalTo(200, containsString("Hello from lein"))));
        assertThat(restClient.deleteApp(leinApp.name), equalTo(200, containsString("{")));
        assertThat(getAllApps().getJSONArray("apps").length(), is(1));
    }

    @Test
    public void nodeAppsWork() throws Exception {
        NodeRunnerTest.ignoreTestIfNotSupported();
        createAndDeploy(nodeApp);
        assertThat(restClient.homepage(nodeApp.name),
            is(equalTo(200, containsString("Hello from nodejs!"))));
        assertThat(restClient.deleteApp(nodeApp.name), equalTo(200, containsString("{")));
        assertThat(getAllApps().getJSONArray("apps").length(), is(1));
    }

    @Test
    public void canUpdateAppsByDeployingThem() throws Exception {
        AppRepo changesApp = AppRepo.create("maven");
        restClient.createApp(changesApp.gitUrl(), "changes-app");
        restClient.deploy("changes-app");

        assertThat(restClient.homepage("changes-app"), is(equalTo(200, containsString("My Maven App"))));

        updateHeaderAndCommit(changesApp, "My new and improved maven app!");

        assertThat(restClient.deploy("changes-app"),
            is(equalTo(200, containsString("buildLogUrl"))));

        JSONObject appInfo = new JSONObject(client.GET(appRunnerUrl + "/api/v1/apps/changes-app").getContentAsString());

        assertThat(
            client.GET(appInfo.getString("url")),
            is(equalTo(200, containsString("My new and improved maven app!"))));

        assertThat(
            client.GET(appInfo.getString("buildLogUrl")),
            is(equalTo(200, containsString("[INFO] Building my-maven-app 1.0-SNAPSHOT"))));
        assertThat(
            client.GET(appInfo.getString("consoleLogUrl")),
            is(equalTo(200, containsString("Starting changes-app in prod"))));

        restClient.deleteApp("changes-app");
    }

    @Test
    public void appRunnerCanStartEvenWithInvalidApps() throws Exception {
        shutDownAppRunner();

        GitRepoLoader repoLoader = FileBasedGitRepoLoader.getGitRepoLoader(dataDir);
        repoLoader.save("invalid-app", "invalid-url");

        buildAndStartUberJar(Collections.emptyList());

        assertThat(restClient.homepage(mavenApp.name), is(equalTo(200, containsString("My Maven App"))));
        JSONObject allApps = getAllApps();
        assertThat("Actual apps: " + allApps.toString(4), allApps.getJSONArray("apps").length(), is(1));
    }


    @Test
    public void stoppedAppsSayTheyAreStopped() throws Exception {
        try {
            restClient.createApp(mavenApp.gitUrl(), "maven-status-test");
            assertMavenAppAvailable("maven-status-test", false, "Not started");
            restClient.deploy("maven-status-test");
            assertMavenAppAvailable("maven-status-test", true, "Running");
            restClient.stop("maven-status-test");
            assertMavenAppAvailable("maven-status-test", false, "Stopped");

            restClient.deploy("maven-status-test");
            assertMavenAppAvailable("maven-status-test", true, "Running");

            // Detecting crashed apps not supported yet
//            crash app
//            assertMavenAppAvailable("maven-status-test", false, "Crashed");
        } finally {
            restClient.deleteApp("maven-status-test");
        }
    }

    private static void assertMavenAppAvailable(String appName, boolean available, String message) throws InterruptedException, ExecutionException, TimeoutException {
        JSONObject appInfo = new JSONObject(client.GET(appRunnerUrl + "/api/v1/apps/" + appName).getContentAsString());
        assertThat(appInfo.getBoolean("available"), is(available));
        assertThat(appInfo.getString("availableStatus"), is(message));
    }

    private static void updateHeaderAndCommit(AppRepo mavenApp, String replacement) throws IOException, GitAPIException {
        File indexHtml = new File(mavenApp.originDir, FilenameUtils.separatorsToSystem("src/main/resources/web/index.html"));
        String newVersion = FileUtils.readFileToString(indexHtml, "UTF-8").replaceAll("<h1>.*</h1>", "<h1>" + replacement + "</h1>");
        FileUtils.write(indexHtml, newVersion, "UTF-8", false);
        mavenApp.origin.add().addFilepattern(".").call();
        mavenApp.origin.commit().setMessage("Updated index.html").setAuthor("Dan F", "danf@example.org").call();
    }

    @Test
    public void theRestAPILives() throws Exception {
        JSONObject all = getAllApps();
        assertThat(all.getInt("appCount"), is(1));
        JSONAssert.assertEquals("{apps:[" +
            "{ name: \"maven\", url: \"" + appRunnerUrl + "/maven/\" }" +
            "]}", all, JSONCompareMode.LENIENT);

        assertThat(restClient.deploy("invalid-app-name"),
            is(equalTo(404, is("No app found with name 'invalid-app-name'. Valid names: maven"))));

        ContentResponse resp = client.GET(appRunnerUrl + "/api/v1/apps/maven");
        assertThat(resp.getStatus(), is(200));
        JSONObject single = new JSONObject(resp.getContentAsString());
        JSONAssert.assertEquals(all.getJSONArray("apps").getJSONObject(0), single, JSONCompareMode.STRICT_ORDER);
    }

    private static JSONObject getAllApps() throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse resp = client.GET(appRunnerUrl + "/api/v1/apps");
        assertThat(resp.getStatus(), is(200));
        return new JSONObject(resp.getContentAsString());
    }


    @Test
    public void appsCanBeDeleted() throws Exception {

        AppRepo newMavenApp = AppRepo.create("maven");
        updateHeaderAndCommit(newMavenApp, "Different repo");
        assertThat(restClient.createApp(newMavenApp.gitUrl(), "another-app").getStatus(), is(201));
        restClient.deploy(newMavenApp.name);

        assertThat(getAllApps().getJSONArray("apps").length(), is(2));

        assertThat(
            restClient.deleteApp("another-app"),
            is(equalTo(200, containsString("another-app"))));
        assertThat(getAllApps().getJSONArray("apps").length(), is(1));
    }

    @Test
    public void appsCanHaveTheirGitUrlsUpdated() throws Exception {

        AppRepo originalApp = AppRepo.create("maven");
        updateHeaderAndCommit(originalApp, "From original repo");
        assertThat(restClient.createApp(originalApp.gitUrl(), "some-app"), equalTo(201, containsString(originalApp.gitUrl())));
        restClient.deploy("some-app");
        assertThat(
            restClient.homepage("some-app"),
            is(equalTo(200, containsString("From original repo"))));

        AppRepo changedApp = AppRepo.create("maven");
        updateHeaderAndCommit(changedApp, "From changed repo");
        assertThat(restClient.updateApp(changedApp.gitUrl(), "some-app"), equalTo(200, containsString(changedApp.gitUrl())));
        restClient.deploy("some-app");
        assertThat(
            restClient.homepage("some-app"),
            is(equalTo(200, containsString("From changed repo"))));


        assertThat(restClient.updateApp(changedApp.gitUrl(), "this-does-not-exist"), equalTo(404, containsString("No application called this-does-not-exist exists")));
        assertThat(restClient.updateApp("", "some-app"), equalTo(400, containsString("No git URL was specified")));
        assertThat(restClient.createApp(originalApp.gitUrl(), "some-app"), equalTo(400, containsString("There is already an app with that ID")));
        restClient.deleteApp("some-app");
        assertThat(getAllApps().getJSONArray("apps").length(), is(1));
    }

    @Test
    public void appsCanGetAuthors() throws Exception {
        ContentResponse resp = client.GET(appRunnerUrl + "/api/v1/apps/maven");
        JSONObject respJson = new JSONObject(resp.getContentAsString());
        assertThat(
            respJson.getString("contributors"),
            is("Author Test"));
    }

    @Test
    public void theSystemApiReturnsOsInfoAndZipsOfSampleProjects() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        JSONObject sysInfo = new JSONObject(client.GET(appRunnerUrl + "/api/v1/system").getContentAsString());

        assertThat(sysInfo.getString("appRunnerVersion"), startsWith("1."));
        assertThat(sysInfo.get("host"), is(notNullValue()));
        assertThat(sysInfo.get("user"), is(notNullValue()));
        assertThat(sysInfo.get("os"), is(notNullValue()));

        JSONArray samples = sysInfo.getJSONArray("samples");
        for (Object app : samples) {
            JSONObject json = (JSONObject) app;

            if (json.getString("name").equalsIgnoreCase("maven")) {
                JSONAssert.assertEquals(
                    "{ name: 'maven', runCommands: [ 'mvn clean package', 'java -jar target/{artifactid}-{version}.jar' ] }",
                    json, JSONCompareMode.LENIENT);
            }

            String url = json.getString("url");
            ContentResponse zip = client.GET(url);
            assertThat(url, zip.getStatus(), is(200));
            assertThat(url, zip.getHeaders().get("Content-Type"), is("application/zip"));
        }

        assertThat(client.GET(appRunnerUrl + "/api/v1/system/samples/badname.zip").getStatus(), is((404)));
    }

    @Test
    public void theSwaggerJSONDescribesTheAPI() throws Exception {
        ContentResponse swagger = restClient.get("/api/v1/swagger.json");
        assertThat(swagger.getStatus(), is(200));
        System.out.println("swagger.getContentAsString() = " + swagger.getContentAsString());
        JSONAssert.assertEquals("{ basePath: '/api/v1'," +
            "paths: {" +
            "'/apps': {}," +
            "'/system': {}" +
            "}" +
            "}", swagger.getContentAsString(), JSONCompareMode.LENIENT);
    }
}
