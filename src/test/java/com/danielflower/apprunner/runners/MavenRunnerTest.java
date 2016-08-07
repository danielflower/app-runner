package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Test;
import scaffolding.Photocopier;
import scaffolding.TestConfig;

import java.io.File;

import static com.danielflower.apprunner.web.WebServer.getAFreePort;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static scaffolding.RestClient.httpClient;

public class MavenRunnerTest {

    private final StringBuilderWriter buildLog = new StringBuilderWriter();
    private final StringBuilderWriter consoleLog = new StringBuilderWriter();

    @Test
    public void canStartAMavenProcessByPackagingAndRunning() throws Exception {
        String appName = "maven";
        MavenRunner runner = new MavenRunner(
            Photocopier.copySampleAppToTempDir(appName),
            HomeProvider.default_java_home,
            MavenRunner.CLEAN_AND_PACKAGE);

        try {
            int port = getAFreePort();
            startAndStop(appName, runner, port);
            startAndStop(appName, runner, port);
        } catch (ProjectCannotStartException e) {
            System.out.println(buildLog);
            System.out.println(consoleLog);
            throw e;
        }
    }

    @Test
    public void theVersionIsReported() {
        MavenRunner runner = new MavenRunner(new File("target"), HomeProvider.default_java_home, MavenRunner.CLEAN_AND_PACKAGE);
        assertThat(runner.getVersionInfo(), anyOf(containsString("Apache Maven"), equalTo("Not available")));
    }

    private void startAndStop(String appName, MavenRunner runner, int port) throws Exception {
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
            assertThat(resp.getContentAsString(), containsString("My Maven App"));
            assertThat(buildLog.toString(), containsString("[INFO] Building my-maven-app 1.0-SNAPSHOT"));
        } finally {
            runner.shutdown();
        }
    }
}
