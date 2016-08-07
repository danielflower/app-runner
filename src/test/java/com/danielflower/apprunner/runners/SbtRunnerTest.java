package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.client.api.ContentResponse;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.Photocopier;
import scaffolding.TestConfig;

import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static scaffolding.RestClient.httpClient;

public class SbtRunnerTest {

    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass public static void ignoreTestIfNoSBT() throws Exception {
        SbtRunner runner = new SbtRunner(new File("target"), HomeProvider.default_java_home, CommandLineProvider.sbt_on_path);
        assumeThat("Skipping SBT test because Scala not detected", runner.getVersionInfo(), not(Matchers.equalTo("Not available")));
    }


    @Test
    public void canStartAndStopSbtProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        canStartAnSBTProject(1);
        canStartAnSBTProject(2);
    }

    @Test
    public void theVersionIsReported() {
        SbtRunner runner = new SbtRunner(new File("target"), HomeProvider.default_java_home, CommandLineProvider.sbt_on_path);
        assertThat(runner.getVersionInfo(), containsString("Scala"));
    }

    private void canStartAnSBTProject(int attempt) throws Exception {
        String appName = "sbt";
        AppRunner runner = new SbtRunner(
            Photocopier.copySampleAppToTempDir(appName),
            HomeProvider.default_java_home,
            CommandLineProvider.sbt_on_path);

        int port = 45678;
        startAndStop(attempt, appName, runner, port, buildLog, consoleLog, containsString("Say hello to akka-http"), containsString("All tests passed"));
    }

    static void startAndStop(int attempt, String appName, AppRunner runner, int port, StringBuilderWriter buildLog, StringBuilderWriter consoleLog, Matcher<String> getResponseMatcher, Matcher<String> buildLogMatcher) throws Exception {
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
                assertThat(resp.getContentAsString(), getResponseMatcher);
                assertThat(buildLog.toString(), buildLogMatcher);
            } finally {
                runner.shutdown();
            }
        } catch (Exception e) {
            clearlyShowError(attempt, e, buildLog, consoleLog);
        }
    }

    static void clearlyShowError(int attempt, Exception e, StringBuilderWriter buildLog, StringBuilderWriter consoleLog) throws Exception {
        System.out.println("Failure on attempt " + attempt);
        System.out.println("Build log");
        System.out.println(buildLog);
        System.out.println();
        System.out.println("Console log");
        System.out.println(consoleLog);
        throw e;
    }
}
