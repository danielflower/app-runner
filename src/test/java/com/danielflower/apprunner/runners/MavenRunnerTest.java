package com.danielflower.apprunner.runners;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.Test;
import scaffolding.Photocopier;

import java.io.File;

import static com.danielflower.apprunner.web.WebServer.getAFreePort;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

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

        int port = getAFreePort();
        startAndStop(1, appName, runner, port);
        startAndStop(2, appName, runner, port);

    }

    @Test
    public void theVersionIsReported() {
        MavenRunner runner = new MavenRunner(new File("target"), HomeProvider.default_java_home, MavenRunner.CLEAN_AND_PACKAGE);
        assertThat(runner.getVersionInfo(), anyOf(containsString("Apache Maven"), equalTo("Not available")));
    }

    private void startAndStop(int attempt, String appName, MavenRunner runner, int port) throws Exception {
        SbtRunnerTest.startAndStop(attempt, appName, runner, port, buildLog, consoleLog, containsString("My Maven App"), containsString("[INFO] Building my-maven-app 1.0-SNAPSHOT"));
    }
}
