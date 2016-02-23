package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class NodeRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(NodeRunner.class);
    private final File projectRoot;
    private final File nodeExec;
    private final File npmExec;
    private ExecuteWatchdog watchDog;

    public NodeRunner(File projectRoot, File nodeExec, File npmExec) {
        this.projectRoot = projectRoot;
        this.nodeExec = nodeExec;
        this.npmExec = npmExec;
    }

    public void start(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {
        runNPM(buildLogHandler, envVarsForApp, "install");
        runNPM(buildLogHandler, envVarsForApp, "test");

        CommandLine command = new CommandLine(nodeExec)
            .addArgument("server.js");

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);
    }

    public void runNPM(InvocationOutputHandler buildLogHandler, Map<String, String> envVarsForApp, String argument) {
        CommandLine command = new CommandLine(nodeExec)
            .addArgument(dirPath(npmExec))
            .addArgument(argument);
        buildLogHandler.consumeLine("Running npm " + argument);
        ProcessStarter.run(buildLogHandler, envVarsForApp, command, projectRoot, TimeUnit.MINUTES.toMillis(20));
    }


    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }

    public static class Factory implements AppRunner.Factory {

        private final File nodeExec;
        private final File npmExec;

        public Factory(File nodeExec, File npmExec) {
            this.nodeExec = nodeExec;
            this.npmExec = npmExec;
        }

        @Override
        public Optional<AppRunner> forProject(String appName, File projectRoot) {
            File projectClj = new File(projectRoot, "package.json");
            return projectClj.isFile()
                ? Optional.of(new NodeRunner(projectRoot, nodeExec, npmExec))
                : Optional.empty();
        }

        public String toString() {
            return "NPM runner for NodeJS apps using " + dirPath(npmExec);
        }
    }

}
