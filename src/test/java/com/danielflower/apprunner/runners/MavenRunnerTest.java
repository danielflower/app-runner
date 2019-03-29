package com.danielflower.apprunner.runners;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.Photocopier;

import java.util.Optional;

import static com.danielflower.apprunner.mgmt.AppManager.getAFreePort;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static scaffolding.TestConfig.config;

public class MavenRunnerTest {

    private static MavenRunnerFactory runnerFactory;
    private final StringBuilderWriter buildLog = new StringBuilderWriter();
    private final StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass
    public static void ignoreTestIfNotSupported() throws Exception {
        Optional<MavenRunnerFactory> runnerFactoryMaybe = MavenRunnerFactory.createIfAvailable(config);
        assumeTrue("Skipping test because maven not detected", runnerFactoryMaybe.isPresent());
        runnerFactory = runnerFactoryMaybe.get();
    }

    @Test
    public void canStartAMavenProcessByPackagingAndRunning() throws Exception {
        String appName = "maven";
        AppRunner runner = runnerFactory.appRunner(Photocopier.copySampleAppToTempDir(appName));
        int port = getAFreePort();
        startAndStop(1, appName, runner, port);
        startAndStop(2, appName, runner, port);
    }

    @Test
    public void theVersionIsReported() {
        assertThat(runnerFactory.versionInfo(), containsString("Apache Maven"));
    }

    private void startAndStop(int attempt, String appName, AppRunner runner, int port) throws Exception {
        ProcessStarterTest.startAndStop(attempt, appName, runner, port, buildLog, consoleLog, containsString("My Maven App"), containsString("[INFO] Building my-maven-app 1.0-SNAPSHOT"));
    }
}
