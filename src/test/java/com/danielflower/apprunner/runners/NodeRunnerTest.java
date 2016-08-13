package com.danielflower.apprunner.runners;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.Photocopier;

import java.util.Optional;

import static com.danielflower.apprunner.runners.ProcessStarterTest.startAndStop;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static scaffolding.TestConfig.config;

public class NodeRunnerTest {

    private static NodeRunnerFactory runnerFactory;
    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass
    public static void ignoreTestIfNotSupported() throws Exception {
        Optional<NodeRunnerFactory> runnerFactoryMaybe = NodeRunnerFactory.createIfAvailable(config);
        assumeTrue("Skipping test because Node and/or NPM not detected", runnerFactoryMaybe.isPresent());
        runnerFactory = runnerFactoryMaybe.get();
    }

    @Test
    public void canStartAndStopNpmProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        run(1);
        run(2);
    }

    @Test
    public void theVersionIsReported() {
        assertThat(runnerFactory.versionInfo(), containsString("Node"));
    }

    private void run(int attempt) throws Exception {
        String appName = "nodejs";
        AppRunner runner = runnerFactory.appRunner(Photocopier.copySampleAppToTempDir(appName));
        startAndStop(attempt, appName, runner, 45688, buildLog, consoleLog, containsString("Hello from nodejs!"), containsString("No test specified"));
    }
}
