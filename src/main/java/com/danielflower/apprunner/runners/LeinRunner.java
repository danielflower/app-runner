package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.danielflower.apprunner.runners.MavenRunner.loadPomModel;

public class LeinRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(LeinRunner.class);
    private final File projectRoot;
    private final CommandLineProvider javaCmd;
    private final CommandLineProvider leinCmd;
    private ExecuteWatchdog watchDog;

    public LeinRunner(File projectRoot, CommandLineProvider javaCmd, CommandLineProvider leinCmd) {
        this.projectRoot = projectRoot;
        this.javaCmd = javaCmd;
        this.leinCmd = leinCmd;
    }

    public void start(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {

        runLein(buildLogHandler, envVarsForApp, "do", "test,", "uberjar,", "pom");

        Model model = loadPomModel(new File(projectRoot, "pom.xml"));
        String jarName = model.getArtifactId() + "-" + model.getVersion() + "-standalone.jar";

        CommandLine command = javaCmd.commandLine(envVarsForApp);
        command.addArgument("-jar").addArgument("target" + File.separator + jarName);

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);
    }

    private void runLein(InvocationOutputHandler buildLogHandler, Map<String, String> envVarsForApp, String... arguments) {
        CommandLine command = leinCmd.commandLine(envVarsForApp);
        for (String argument : arguments)
            command.addArgument(argument);

        buildLogHandler.consumeLine("Running lein " + StringUtils.join(arguments, " ") + " with " + command);
        ProcessStarter.run(buildLogHandler, envVarsForApp, command, projectRoot, TimeUnit.MINUTES.toMillis(20));
    }

    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }

    public static class Factory implements AppRunner.Factory {
        private final CommandLineProvider javaCmd;
        private final CommandLineProvider leinCmd;

        public Factory(CommandLineProvider javaCmd, CommandLineProvider leinCmd) {
            this.javaCmd = javaCmd;
            this.leinCmd = leinCmd;
        }

        public Optional<AppRunner> forProject(String appName, File projectRoot) {
            File projectClj = new File(projectRoot, "project.clj");
            if (projectClj.isFile()) {
                LeinRunner runner = new LeinRunner(projectRoot, javaCmd, leinCmd);
                return Optional.of(runner);
            } else {
                return Optional.empty();
            }
        }

        public String toString() {
            return "Leiningin runner for Clojure apps using " /*+ leinJar.getName()*/;
        }
    }
}
