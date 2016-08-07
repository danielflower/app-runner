package com.danielflower.apprunner.runners;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.Test;
import scaffolding.Photocopier;

import java.io.File;

import static com.danielflower.apprunner.runners.SbtRunnerTest.startAndStop;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

public class LeinRunnerTest {

    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @Test
    public void canStartAndStopLeinProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        canStartALeinProject(1);
        canStartALeinProject(2);
    }

    @Test
    public void theVersionIsReported() {
        LeinRunner runner = new LeinRunner(new File("target"), HomeProvider.default_java_home, CommandLineProvider.lein_on_path);
        assertThat(runner.getVersionInfo(), anyOf(containsString("Lein"), equalTo("Not available")));
    }

    public void canStartALeinProject(int attempt) throws Exception {
        String appName = "lein";
        LeinRunner runner = new LeinRunner(
            Photocopier.copySampleAppToTempDir(appName),
            HomeProvider.default_java_home,
            CommandLineProvider.lein_on_path);
        startAndStop(attempt, appName, runner, 45678, buildLog, consoleLog, containsString("Hello from lein"), containsString("Ran 1 tests containing 1 assertions"));
    }
}
