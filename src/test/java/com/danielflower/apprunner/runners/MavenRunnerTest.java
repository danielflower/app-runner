package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.FileUtils;
import com.danielflower.apprunner.problems.AppRunnerException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MavenRunnerTest {

    private static HttpClient client;

    @BeforeClass
    public static void setup() throws Exception {
        client = new HttpClient(new SslContextFactory(true));
        client.start();
    }

    @AfterClass
    public static void stop() throws Exception {
        client.stop();
    }

    @Test
    public void canStartAMavenProcessByPackagingAndRunning() throws InterruptedException, ExecutionException, TimeoutException {

        MavenRunner runner = new MavenRunner(sampleAppDir("maven"));
        runner.start(45678);

        try {
            ContentResponse resp = client.GET("http://localhost:45678/");
            assertThat(resp.getStatus(), is(200));
            assertThat(resp.getContentAsString(), containsString("My Maven App"));
        } finally {
            runner.shutdown();
        }
    }

    private static File sampleAppDir(String subDir) {
        File samples = new File("src/main/resources/samples/" + subDir);
        if (!samples.isDirectory()) {
            throw new AppRunnerException("Could not find sample dir at " + FileUtils.dirPath(samples));
        }
        return samples;
    }

}
