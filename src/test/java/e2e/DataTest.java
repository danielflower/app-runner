package e2e;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.web.WebServer;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.client.api.ContentResponse;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import scaffolding.AppRepo;
import scaffolding.Photocopier;
import scaffolding.RestClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static scaffolding.ContentResponseMatcher.equalTo;
import static scaffolding.Photocopier.sampleDir;

public class DataTest {

    private final int port = WebServer.getAFreePort();
    private final String appRunnerUrl = "http://localhost:" + port;
    private final RestClient restClient = RestClient.create(appRunnerUrl);
    private final String appId = "maven";
    private final AppRepo appRepo = AppRepo.create(appId);
    private final File appRunnerDataDir = new File("target/datadirs/" + System.currentTimeMillis());
    private final File mavenZip = new File("src/test/maven.zip");

    private final App app = new App(new Config(new HashMap<String,String>() {{
        put(Config.SERVER_HTTP_PORT, String.valueOf(port));
        put(Config.DATA_DIR, fullPath(appRunnerDataDir));
    }}));

    @Before public void start() throws Exception {
        app.start();
        assertThat(restClient.createApp(appRepo.gitUrl()).getStatus(), is(201));
        assertThat("File exists? " + mavenZip.getCanonicalPath(), mavenZip.isFile(), is(true));
    }

    @After public void shutdownApp() {
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
    public void ifNoFilesThenAnEmptyZipIsReturned() throws Exception {
        ContentResponse resp = restClient.getData(appId);
        assertThat(resp.getStatus(), is(200));
        assertThat(resp.getHeaders().get("Content-Type"), CoreMatchers.equalTo("application/zip"));

        List<String> pathsInZip = getFilesInZip(resp);
        assertThat(pathsInZip, hasSize(0));
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
