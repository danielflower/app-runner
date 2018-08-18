package com.danielflower.apprunner.runners;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.Photocopier;

import java.util.Optional;

import static com.danielflower.apprunner.web.WebServer.getAFreePort;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static scaffolding.TestConfig.config;

public class DotnetRunnerTest {

    private static DotnetRunnerFactory runnerFactory;
    private final StringBuilderWriter buildLog = new StringBuilderWriter();
    private final StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass
    public static void ignoreTestIfNotSupported() throws Exception {
        Optional<DotnetRunnerFactory> runnerFactoryMaybe = DotnetRunnerFactory.createIfAvailable(config);
        assumeTrue("Skipping test because .NET Core SDK not detected", runnerFactoryMaybe.isPresent());
        runnerFactory = runnerFactoryMaybe.get();
    }

    @Test
    public void canRunADotnetCoreProject() throws Exception {
        String appName = "dotnet";
        AppRunner runner = runnerFactory.appRunner(Photocopier.copySampleAppToTempDir(appName));
        int port = getAFreePort();

        ProcessStarterTest.startAndStop(1, appName, runner, port, buildLog, consoleLog, containsString(".NET Core sample project"), containsString("Dotnet app started"));
    }

    @Test
    public void theVersionIsReported() {
        assertThat(runnerFactory.versionInfo(), containsString(".NET Core SDK"));
    }

    private void startAndStop(int attempt, String appName, AppRunner runner, int port) throws Exception {
    }
}
