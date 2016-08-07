package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Test;
import scaffolding.Photocopier;
import scaffolding.TestConfig;

import java.io.File;

import static com.danielflower.apprunner.runners.SbtRunnerTest.clearlyShowError;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static scaffolding.RestClient.httpClient;
import static scaffolding.TestConfig.config;

public class NodeRunnerTest {

    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @Test
    public void canStartAndStopNpmProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        run(1);
        run(2);
    }

    @Test
    public void theVersionIsReported() {
        NodeRunner runner = new NodeRunner(new File("target"), config.nodeExecutable(), config.npmExecutable());
        assertThat(runner.getVersionInfo(), anyOf(containsString("Node"), equalTo("Not available")));
    }

    private void run(int attempt) throws Exception {
        String appName = "nodejs";
        NodeRunner runner = new NodeRunner(
            Photocopier.copySampleAppToTempDir(appName),
            config.nodeExecutable(),
            config.npmExecutable());

        int port = 45688;
        try {
            try (Waiter startupWaiter = Waiter.waitForApp(appName, port)) {
                runner.start(
                    new OutputToWriterBridge(buildLog),
                    new OutputToWriterBridge(consoleLog),
                    TestConfig.testEnvVars(port, appName),
                    startupWaiter);
            }
            try {
                ContentResponse resp = httpClient.GET("http://localhost:" + port + "/" + appName + "/");
                assertThat(resp.getStatus(), is(200));
                assertThat(resp.getContentAsString(), containsString("Hello from nodejs!"));
                assertThat(buildLog.toString(), containsString("No test specified"));
            } finally {
                runner.shutdown();
            }
        } catch (Exception e) {
            clearlyShowError(attempt, e, buildLog, consoleLog);
        }
    }
}
