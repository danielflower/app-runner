package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.Photocopier;
import scaffolding.TestConfig;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LeinRunnerTest {

    private static HttpClient client;
    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass
    public static void setup() throws Exception {
//        Assume.assumeTrue("Skipping tests as LEIN not detected", config.leinJar().isPresent());

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
    public void canStartAndStopLeinProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        canStartALeinProject(1);
        canStartALeinProject(2);
    }

    public void canStartALeinProject(int attempt) throws Exception {
        String appName = "lein";
        LeinRunner runner = new LeinRunner(Photocopier.copySampleAppToTempDir(appName), HomeProvider.default_java_home, CommandLineProvider.lein_on_path);
//        LeinRunner runner = new LeinRunner(sampleAppDir(appName), null, tempDir, JavaHomeProvider.default_java_home);
        int port = 45678;
        try {
            try (Waiter startupWaiter = Waiter.waitForApp(appName, port)) {
                runner.start(new OutputToWriterBridge(buildLog), new OutputToWriterBridge(consoleLog),
                    TestConfig.testEnvVars(port, appName), startupWaiter);
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
