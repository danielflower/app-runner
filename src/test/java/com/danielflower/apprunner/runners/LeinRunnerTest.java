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

public class LeinRunnerTest {

    private static LeinRunnerFactory runnerFactory;
    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass
    public static void ignoreTestIfNotSupported() throws Exception {
        Optional<LeinRunnerFactory> runnerFactoryMaybe = LeinRunnerFactory.createIfAvailable(config);
        assumeTrue("Skipping test because lein not detected", runnerFactoryMaybe.isPresent());
        runnerFactory = runnerFactoryMaybe.get();
    }

    @Test
    public void canStartAndStopLeinProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        canStartALeinProject(1);
        canStartALeinProject(2);
    }

    @Test
    public void theVersionIsReported() {
        assertThat(runnerFactory.versionInfo(), containsString("Lein"));
    }

    private void canStartALeinProject(int attempt) throws Exception {
        String appName = "lein";
        AppRunner runner = runnerFactory.appRunner(Photocopier.copySampleAppToTempDir(appName));
        startAndStop(attempt, appName, runner, 45678, buildLog, consoleLog, containsString("Hello from lein"), containsString("Ran 1 tests containing 1 assertions"));
    }
}
