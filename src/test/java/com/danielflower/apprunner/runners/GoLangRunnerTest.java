package com.danielflower.apprunner.runners;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.Photocopier;
import scaffolding.TestConfig;

import java.io.File;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;

public class GoLangRunnerTest {

    private static GoRunnerFactory goRunnerFactory;
    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass public static void ignoreTestIfNotSupported() throws Exception {
        Optional<GoRunnerFactory> goRunnerFactoryMaybe = GoRunnerFactory.createIfAvailable(TestConfig.config);
        assumeTrue("Skipping GO test because Go not detected", goRunnerFactoryMaybe.isPresent());
        goRunnerFactory = goRunnerFactoryMaybe.get();
    }

    @Test
    public void canStartAndStopGoProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        canStartAnGOProject(1);
        canStartAnGOProject(2);
    }

    @Test
    public void theVersionIsReported() {
        assertThat(goRunnerFactory.versionInfo(), containsString("go"));
    }

    private void canStartAnGOProject(int attempt) throws Exception {
        String appName = "golang";
        File projectRoot = Photocopier.folderForSampleProject(appName+"_workspace" + File.separator + "src" + File.separator + appName);
        FileUtils.copyDirectory(new File(Photocopier.sampleDir(), appName),
                projectRoot);
        AppRunner runner = goRunnerFactory.appRunner(projectRoot);
        ProcessStarterTest.startAndStop(attempt, appName, runner, 45678, buildLog, consoleLog, containsString("Welcome!\n"), containsString("PASS"));
    }

}
