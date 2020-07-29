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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.moandjiezana.toml.Toml;

public class RustRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(RustRunner.class);
    public static final String[] startCommands = new String[]{"cargo build", "cargo test", "mv ./target/debug/<app_name> ./<app_name>", "cargo clean", "./<app_name>"};

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

        //Move built executable so that we can clean up the rest of the build files
        final String compiledExe = "target/debug/" + rustPackageName + (SystemUtils.IS_OS_WINDOWS ? ".exe" : ""); //Stick to debug builds until someone has a performance issue
        final String exeToRun = "./" + rustPackageName + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");

        Path sourcePath = Paths.get(projectRoot + "/" + compiledExe).toAbsolutePath();
        Path targetPath = Paths.get(projectRoot + "/" + exeToRun).toAbsolutePath();

        try {
            Files.move(sourcePath, targetPath);
        }
        catch(IOException ioe){
            throw new ProjectCannotStartException("Couldn't move compiled binary from " + sourcePath.toString() + " to " + targetPath.toString(), ioe);
        }
        runCargo(buildLogHandler, envVarsForApp, "clean");

        //Run the app
        CommandLine command = new CommandLine(exeToRun);
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
