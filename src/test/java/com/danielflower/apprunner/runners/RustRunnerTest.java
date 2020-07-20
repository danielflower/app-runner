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

public class RustRunnerTest {

    private static RustRunnerFactory runnerFactory;
    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass
    public static void ignoreTestIfNotSupported() throws Exception {
        Optional<RustRunnerFactory> runnerFactoryMaybe = RustRunnerFactory.createIfAvailable(config);
        assumeTrue("Skipping rust test because cargo not detected", runnerFactoryMaybe.isPresent());
        runnerFactory = runnerFactoryMaybe.get();
    }

    @Test
    public void canStartAndStopCargoProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        run(1);
        run(2);
    }

    @Test
    public void theVersionIsReported() {
        assertThat(runnerFactory.versionInfo(), containsString("cargo"));
    }

    private void run(int attempt) throws Exception {
        String appName = "rust";
        AppRunner runner = runnerFactory.appRunner(Photocopier.copySampleAppToTempDir(appName));
        startAndStop(attempt, appName, runner, 45688, buildLog, consoleLog, containsString("Rust sample app is running"), containsString("test result: ok"));
    }
}
