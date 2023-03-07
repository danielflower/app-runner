package com.danielflower.apprunner.runners;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.Photocopier;
import scaffolding.TestConfig;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;

public class SbtRunnerTest {

    private static SbtRunnerFactory sbtRunnerFactory;
    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass public static void ignoreTestIfNotSupported() throws Exception {
        Optional<SbtRunnerFactory> sbtRunnerFactoryMaybe = SbtRunnerFactory.createIfAvailable(TestConfig.config);
        assumeTrue("Skipping SBT test because Scala not detected", sbtRunnerFactoryMaybe.isPresent());
        sbtRunnerFactory = sbtRunnerFactoryMaybe.get();
    }

    @Test
    public void canStartAndStopSbtProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        canStartAnSBTProject(1);
        canStartAnSBTProject(2);
    }

    @Test
    public void theVersionIsReported() {
        assertThat(sbtRunnerFactory.versionInfo(), containsString("Scala"));
    }

    private void canStartAnSBTProject(int attempt) throws Exception {
        String appName = "sbt";
        AppRunner runner = sbtRunnerFactory.appRunner(Photocopier.copySampleAppToTempDir(appName));
        ProcessStarterTest.startAndStop(attempt, appName, runner, 45678, buildLog, consoleLog, containsString("Say hello to akka-http"), containsString("All tests passed"));
    }

}
