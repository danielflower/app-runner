package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NodeRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(NodeRunner.class);

    private final File projectRoot;
    private final String nodeExec;
    private final String npmExec;
    private ExecuteWatchdog watchDog;

    public NodeRunner(File projectRoot, String nodeExec, String npmExec) {
        this.projectRoot = projectRoot;
        this.nodeExec = nodeExec;
        this.npmExec = npmExec;
    }

    public void start(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {
        runNPM(buildLogHandler, envVarsForApp, "install");
        runNPM(buildLogHandler, envVarsForApp, "test");

        CommandLine command = new CommandLine(nodeExec).addArgument("server.js");

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);
    }

    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }

    private void runNPM(InvocationOutputHandler buildLogHandler, Map<String, String> envVarsForApp, String argument) {
        CommandLine command = new CommandLine(npmExec).addArgument(argument);

        buildLogHandler.consumeLine("Running " + StringUtils.join(command.toStrings(), " "));
        ProcessStarter.run(buildLogHandler, envVarsForApp, command, projectRoot, TimeUnit.MINUTES.toMillis(30));
    }
}
