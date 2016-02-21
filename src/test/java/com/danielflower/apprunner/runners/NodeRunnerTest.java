package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import com.danielflower.apprunner.mgmt.AppManager;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.*;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.danielflower.apprunner.runners.MavenRunnerTest.sampleAppDir;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.TestConfig.config;

public class NodeRunnerTest {

    private static HttpClient client;
    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass
    public static void setup() throws Exception {
        Assume.assumeTrue("Skipping tests as NPM not detected", config.nodeExecutable().isPresent());

        client = new HttpClient();
        client.start();
    }

    @AfterClass
    public static void stop() throws Exception {
        if (client != null) {
            client.stop();
        }
    }

    @Test
    public void canStartAndStopNpmProjects() throws InterruptedException, ExecutionException, TimeoutException {
        // doing it twice proves the port was cleaned up
        run(1);
        run(2);
    }

    public void run(int attempt) throws InterruptedException, ExecutionException, TimeoutException {

        String appName = "nodejs";
        NodeRunner runner = new NodeRunner(sampleAppDir(appName), config.nodeExecutable().get(), config.npmExecutable().get());
        int port = 45688;
        try {
            runner.start(new OutputToWriterBridge(buildLog), new OutputToWriterBridge(consoleLog),
                AppManager.createAppEnvVars(port, appName, URI.create("http://localhost")));

            try {
                ContentResponse resp = client.GET("http://localhost:" + port + "/" + appName + "/");
                assertThat(resp.getStatus(), is(200));
                assertThat(resp.getContentAsString(), containsString("Hello from nodejs!"));
                assertThat(buildLog.toString(), containsString("Running npm install"));
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
