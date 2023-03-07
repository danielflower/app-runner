package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.mgmt.AppManager;
import org.eclipse.jetty.client.api.ContentResponse;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.AppRepo;
import scaffolding.RestClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class DataTest {

    private final static AtomicInteger appCount = new AtomicInteger();
    private final static int port = AppManager.getAFreePort();
    private final static RestClient restClient = RestClient.create("http://localhost:" + port);
    private final String appId = "maven-app-" + appCount.incrementAndGet();
    private static final File mavenZip = new File("src/test/maven.zip");
    private static final File emptyZip = new File("src/test/empty.zip");
    private static final File pomXml = new File("pom.xml");

    private static final String dataDir = fullPath(new File("target/datadirs/" + System.currentTimeMillis()));
    private static final App app = new App(new Config(new HashMap<String,String>() {{
        put(Config.M2_HOME, System.getenv("M2_HOME"));
        put(Config.SERVER_HTTP_PORT, String.valueOf(port));
        put(Config.DATA_DIR, dataDir);
    }}));

    @BeforeClass
    public static void startServer() throws Exception {
        app.start();
        assertThat("File exists? " + mavenZip.getCanonicalPath(), mavenZip.isFile(), is(true));
        assertThat("File exists? " + emptyZip.getCanonicalPath(), emptyZip.isFile(), is(true));
        assertThat("File exists? " + pomXml.getCanonicalPath(), pomXml.isFile(), is(true));
    }
    @Before public void start() throws Exception {
        AppRepo appRepo = AppRepo.create(appId, "maven");
        ContentResponse resp = restClient.createApp(appRepo.gitUrl(), appId);
        assertThat("Repsonse body: " + resp.getContentAsString(), resp.getStatus(), is(201));
    }

    @AfterClass
    public static void shutdownApp() {
        app.shutdown();
    }

    @Test
    public void filesInTheAppsDataDirCanBeUploadedAndDownloadedAsAZip() throws Exception {

        ContentResponse uploadResp = restClient.postData(appId, mavenZip);
        assertThat(uploadResp.getStatus(), is(204));

        ContentResponse resp = restClient.getData(appId);
        assertThat(resp.getStatus(), is(200));
        assertThat(resp.getHeaders().get("Content-Type"), CoreMatchers.equalTo("application/zip"));

        List<String> pathsInZip = getFilesInZip(resp);
        assertThat(pathsInZip, not(hasItem("instances/")));
        assertThat(pathsInZip, hasItem("pom.xml"));
        assertThat(pathsInZip, hasItem("src/main/java/samples/App.java"));
    }

    @Test
    public void filesCanBeDeleted() throws Exception {
        ContentResponse uploadResp = restClient.postData(appId, mavenZip);
        assertThat(uploadResp.getStatus(), is(204));

        ContentResponse resp = restClient.deleteData(appId);
        assertThat(resp.getStatus(), is(204));
        assertThat(getFilesInZip(resp), hasSize(0));
    }

    @Test
    public void whenTheAppIsDeletedTheFilesAreDeletedToo() throws Exception {
        ContentResponse uploadResp = restClient.postData(appId, mavenZip);
        assertThat(uploadResp.getStatus(), is(204));

        ContentResponse resp = restClient.deleteApp(appId);
        assertThat(resp.getStatus(), is(200));

        File appDataDir = new File(dataDir, "apps/" + appId);
        assertThat(appDataDir.exists(), is(false));
    }

    @Test
    public void ifAnyFilesExistThenA400IsReturned() throws Exception {
        restClient.postData(appId, mavenZip);
        ContentResponse uploadResp = restClient.postData(appId, mavenZip);
        assertThat(uploadResp.getStatus(), is(400));
        assertThat(uploadResp.getContentAsString(), CoreMatchers.equalTo("File uploading is only supported for apps with empty data directories."));
    }

    @Test
    public void ifTheFileIsNotAZipA204IsReturnedButNothingIsSavedBecauseIDidNotSeeHowToDifferentiateBetweenAnEmptyZipAndANonZipEasily() throws Exception {
        ContentResponse uploadResp = restClient.postData(appId, pomXml);
        assertThat(uploadResp.getStatus(), is(204));
        assertThat(getFilesInZip(restClient.getData(appId)), hasSize(0));
    }

    @Test
    public void ifNoFilesThenAnEmptyZipIsReturned() throws Exception {
        ContentResponse resp = restClient.getData(appId);
        assertThat(resp.getStatus(), is(200));
        assertThat(resp.getHeaders().get("Content-Type"), CoreMatchers.equalTo("application/zip"));
        List<String> pathsInZip = getFilesInZip(resp);
        assertThat(pathsInZip, hasSize(0));
    }

    @Test
    public void uploadingAnEmptyZipHasNoImpactAndA204IsReturned() throws Exception {
        ContentResponse uploadResp = restClient.postData(appId, emptyZip);
        assertThat(uploadResp.getStatus(), is(204));

        ContentResponse resp = restClient.getData(appId);
        assertThat(getFilesInZip(resp), hasSize(0));
    }


    private static List<String> getFilesInZip(ContentResponse resp) throws IOException {
        byte[] content = resp.getContent();
        List<String> pathsInZip = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry nextEntry;
            while ((nextEntry = zis.getNextEntry()) != null) {
                pathsInZip.add(nextEntry.getName());
            }
        }
        return pathsInZip;
    }


}
