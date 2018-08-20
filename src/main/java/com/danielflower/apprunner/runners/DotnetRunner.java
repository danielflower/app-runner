package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.LineConsumer;
import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class DotnetRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(PythonRunner.class);

    private final File projectRoot;
    private final File projectFile;
    private final String executable;
    private ExecuteWatchdog watchDog;
    public static final String[] startCommands = new String[] { "dotnet run" };

    public DotnetRunner(File projectRoot, File projectFile, String executable) {
        this.projectRoot = projectRoot;
        this.projectFile = projectFile;
        this.executable = executable;
    }

    @Override
    public File getInstanceDir() {
        return projectRoot;
    }

    @Override
    public void start(LineConsumer buildLogHandler, LineConsumer consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {

        CommandLine command = new CommandLine(executable)
            .addArgument("run")
            .addArgument("-p")
            .addArgument(projectFile.getName());

        buildLogHandler.consumeLine("Starting Dotnet app: " + StringUtils.join(command.toStrings(), " "));

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);

        buildLogHandler.consumeLine("Dotnet app started.");
    }

    @Override
    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }
}
