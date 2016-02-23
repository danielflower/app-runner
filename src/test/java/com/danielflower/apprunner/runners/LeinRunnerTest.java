package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import com.danielflower.apprunner.mgmt.AppManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.danielflower.apprunner.runners.MavenRunnerTest.sampleAppDir;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.TestConfig.config;

public class LeinRunnerTest {

    private static HttpClient client;
    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();
    private static File tempDir = new File("target/temp");

    @BeforeClass
    public static void setup() throws Exception {
        Assume.assumeTrue("Skipping tests as LEIN not detected", config.leinJar().isPresent());

        client = new HttpClient();
        client.start();
        FileUtils.forceMkdir(tempDir);
    }

    @AfterClass
    public static void stop() throws Exception {
        if (client != null) {
            client.stop();
        }
    }

    @Test
    public void canStartAndStopLeinProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        canStartALeinProject(1);
        canStartALeinProject(2);
    }

    public void canStartALeinProject(int attempt) throws Exception {

        String appName = "lein";
        LeinRunner runner = new LeinRunner(sampleAppDir(appName), config.leinJar().get(), config.leinJavaExecutable(), tempDir);
        int port = 45678;
        try {
            try (Waiter startupWaiter = Waiter.waitForApp(appName, port)) {
                runner.start(new OutputToWriterBridge(buildLog), new OutputToWriterBridge(consoleLog),
                    AppManager.createAppEnvVars(port, appName, URI.create("http://localhost")), startupWaiter);
            }
            try {
                ContentResponse resp = client.GET("http://localhost:" + port + "/" + appName + "/");
                assertThat(resp.getStatus(), is(200));
                assertThat(resp.getContentAsString(), containsString("Hello from lein"));
                assertThat(buildLog.toString(), containsString("Ran 1 tests containing 1 assertions"));
            } finally {
                runner.shutdown();
            }
        } catch (Exception e) {
            System.out.println("Failure on attempt " + attempt);
            System.out.println("Build log");
            System.out.println(buildLog);
            System.out.println();
            System.out.println("Console log");
            System.out.println(consoleLog);
            throw e;
        }

    }


}
