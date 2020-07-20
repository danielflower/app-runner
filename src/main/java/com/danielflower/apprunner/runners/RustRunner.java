package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.LineConsumer;
import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.moandjiezana.toml.Toml;

public class RustRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(RustRunner.class);
    public static final String[] startCommands = new String[]{"cargo build", "cargo test", "./target/debug/<app_name>"};

    private final File projectRoot;
    private final String cargoExec;
    private ExecuteWatchdog watchDog;

    public RustRunner(File projectRoot, String cargoExec) {
        this.projectRoot = projectRoot;
        this.cargoExec = cargoExec;
    }

    @Override
    public File getInstanceDir() {
        return projectRoot;
    }

    public void start(LineConsumer buildLogHandler, LineConsumer consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {
        Toml toml = new Toml().read(new File(projectRoot, "Cargo.toml"));
        String rustPackageName = toml.getString("package.name");

        runCargo(buildLogHandler, envVarsForApp, "build");
        runCargo(buildLogHandler, envVarsForApp, "test");

        final String COMPILED_EXE = "target/debug/" + rustPackageName; //Stick to debug builds until someone has a performance issue
        CommandLine command = new CommandLine(SystemUtils.IS_OS_WINDOWS ? COMPILED_EXE + ".exe" : COMPILED_EXE);

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);
    }

    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }

    private void runCargo(LineConsumer buildLogHandler, Map<String, String> envVarsForApp, String argument) {
        CommandLine command = new CommandLine(cargoExec).addArgument(argument);

        buildLogHandler.consumeLine("Running " + StringUtils.join(command.toStrings(), " "));
        ProcessStarter.run(buildLogHandler, envVarsForApp, command, projectRoot, TimeUnit.MINUTES.toMillis(30));
    }
}
