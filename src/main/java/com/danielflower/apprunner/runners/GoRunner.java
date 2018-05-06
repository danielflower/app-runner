package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.LineConsumer;
import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GoRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(GoRunner.class);

    public static final String[] startCommands = new String[]{"go get", "go build", "go test", "./{app_dir_name}"};
    private final File projectRoot;
    private final File instanceDir;
    private final CommandLineProvider goCmd;
    private ExecuteWatchdog watchDog;

    /*
     * Go project need special folder layout to build and run
     * e.g.
     * /instances
     *  /{timestamp as random folder name}  <---gopath
     *      /src
     *          /{timestamp as random folder name}
     *              example.go
     *              example_test.go
     *      /bin
     *      /pkg
     */
    public GoRunner(File projectRoot, CommandLineProvider goCmd) {
        if (projectRoot.getAbsolutePath().contains("instances")) {
            this.instanceDir = projectRoot;
            this.projectRoot = new File(projectRoot, "src" + File.separator + projectRoot.getName());
            try {
                FileUtils.copyDirectory(projectRoot, this.projectRoot);
                for (File file : instanceDir.listFiles()) {
                    if (file.isFile()) FileUtils.deleteQuietly(file);
                }
            } catch (IOException e) {
                log.error("Failed to move go project into its sub dir.", e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        } else {
            this.instanceDir = this.projectRoot = projectRoot;
        }
        this.goCmd = goCmd;
    }

    @Override
    public File getInstanceDir() {
        return instanceDir;
    }

    public void start(LineConsumer buildLogHandler, LineConsumer consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {
        envVarsForApp.put("GOPATH", instanceDir.getAbsolutePath());
        rungo(buildLogHandler, envVarsForApp, "get");
        rungo(buildLogHandler, envVarsForApp, "build");
        rungo(buildLogHandler, envVarsForApp, "test");

        CommandLine command = new CommandLine("." + File.separator + projectRoot.getName());

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);
    }

    private void rungo(LineConsumer buildLogHandler, Map<String, String> envVarsForApp, String... arguments) {
        CommandLine command = goCmd.commandLine(envVarsForApp);
        for (String argument : arguments)
            command.addArgument(argument);

        buildLogHandler.consumeLine("Running go " + StringUtils.join(arguments, " ") + " with " + command);
        ProcessStarter.run(buildLogHandler, envVarsForApp, command, projectRoot, TimeUnit.MINUTES.toMillis(20));
    }

    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }
}
