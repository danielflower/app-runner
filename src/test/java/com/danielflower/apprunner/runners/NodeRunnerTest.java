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

        startAndStop(attempt, appName, runner, 45688, buildLog, consoleLog, containsString("Hello from nodejs!"), containsString("No test specified"));
    }
}
