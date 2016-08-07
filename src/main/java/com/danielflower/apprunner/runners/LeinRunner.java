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
import java.util.concurrent.TimeUnit;

import static com.danielflower.apprunner.runners.MavenRunner.loadPomModel;

public class LeinRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(LeinRunner.class);
    public static final String[] startCommands = new String[]{"lein do test, uberjar, pom", "java -jar target/{artifactid}-{version}.jar"};

    private final File projectRoot;
    private final CommandLineProvider javaCmd;
    private final CommandLineProvider leinCmd;
    private ExecuteWatchdog watchDog;

    public LeinRunner(File projectRoot, CommandLineProvider javaCmd, CommandLineProvider leinCmd) {
        this.projectRoot = projectRoot;
        this.javaCmd = javaCmd;
        this.leinCmd = leinCmd;
    }

    @Override
    public File getInstanceDir() {
        return projectRoot;
    }

    @Override
    public String getVersionInfo() {
        return ProcessStarter.run(leinCmd.commandLine(null).addArgument("--version")).getRight();
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
}
