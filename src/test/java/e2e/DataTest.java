package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.web.WebServer;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class DataTest {

    private final static AtomicInteger appCount = new AtomicInteger();
    private final static int port = WebServer.getAFreePort();
    private final static RestClient restClient = RestClient.create("http://localhost:" + port);
    private final String appId = "maven-app-" + appCount.incrementAndGet();
    private static final File mavenZip = new File("src/test/maven.zip");
    private static final File emptyZip = new File("src/test/empty.zip");
    private static final File pomXml = new File("pom.xml");

    private static final App app = new App(new Config(new HashMap<String,String>() {{
        put(Config.SERVER_HTTP_PORT, String.valueOf(port));
        put(Config.DATA_DIR, fullPath(new File("target/datadirs/" + System.currentTimeMillis())));
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
        assertThat(restClient.createApp(appRepo.gitUrl(), appId).getStatus(), is(201));
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
        Files.write(new File("target/blah.zip").toPath(), content);
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
