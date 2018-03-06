package com.danielflower.apprunner.runners;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import scaffolding.Photocopier;

import java.util.Optional;

import static com.danielflower.apprunner.runners.ProcessStarterTest.startAndStop;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static scaffolding.TestConfig.config;

public class PythonRunnerTest {
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(PythonRunnerTest.class);
    private static Optional<PythonRunnerFactory> runnerFactory2;
    private static Optional<PythonRunnerFactory> runnerFactory3;

    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @BeforeClass
    public static void ignoreTestIfNotSupported() throws Exception {
        if (runnerFactory2 == null && runnerFactory3 == null) {
            runnerFactory2 = PythonRunnerFactory.createIfAvailable(config, 2);
            runnerFactory3 = PythonRunnerFactory.createIfAvailable(config, 3);
        }
        assumeTrue("Skipping all Python tests because neither Python 2 or 3 were detected", (runnerFactory2.isPresent() || runnerFactory3.isPresent()));
    }

    public static boolean isPythonVersionDetected(int majorVersion) throws Exception{
        if (runnerFactory2 == null && runnerFactory3 == null){
            ignoreTestIfNotSupported();
        }
        if (majorVersion == 2){
            return runnerFactory2.isPresent();
        }
        else if (majorVersion == 3){
            return runnerFactory3.isPresent();
        }
        else{
            throw new IllegalArgumentException("Only versions 2 and 3 of Python are supported");
        }
    }

    @Test
    public void canStartAndStopPythonProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        if (runnerFactory2.isPresent()) {
            run(runnerFactory2.get(), "python2", "Python 2 in AppRunner", 1);
            run(runnerFactory2.get(), "python2", "Python 2 in AppRunner", 2);
        }
        else{
            log.info("Skipping Python 2 tests, since no Python 2 interpreter was found.");
        }

        if (runnerFactory3.isPresent()) {
            run(runnerFactory3.get(), "python3", "Python 3 in AppRunner", 1);
            run(runnerFactory3.get(), "python3", "Python 3 in AppRunner", 2);
        }
        else{
            log.info("Skipping Python 3 tests, since no Python 3 interpreter was found.");
        }
    }

    @Test
    public void theVersionIsReported() {
        if (runnerFactory2.isPresent()) {
            assertThat(runnerFactory2.get().versionInfo(), containsString("Python 2."));
        }
        if (runnerFactory3.isPresent()) {
            assertThat(runnerFactory3.get().versionInfo(), containsString("Python 3."));
        }
    }

    private void run(PythonRunnerFactory runnerFactory, String appName, String expectedText, int attempt) throws Exception {
        AppRunner runner = runnerFactory.appRunner(Photocopier.copySampleAppToTempDir(appName));
        startAndStop(attempt, appName, runner, 45688, buildLog, consoleLog, containsString(expectedText), containsString("Python app started."));
    }
}
