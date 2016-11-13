package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;

public class SbtRunner implements AppRunner {

    public static final String[] startCommands = new String[]{"sbt clean assembly", "java -jar {artifactPath}"};

    private static final List<String> CLEAN_AND_PACKAGE = asList("clean", "assembly");
    private static final int SBT_LINE_BUFFER = 5;
    private static final Pattern ARTIFACT_PATH_REGEX = Pattern.compile(".*Packaging\\s+(\\S+)\\s+.*", Pattern.DOTALL);

    private final File projectRoot;
    private final CommandLineProvider javaCmd;
    private final CommandLineProvider sbtCmd;

    private ExecuteWatchdog watchDog;

    public SbtRunner(final File projectRoot,
                     final CommandLineProvider javaCmd,
                     final CommandLineProvider sbtCmd) {
        this.projectRoot = projectRoot;
        this.javaCmd = javaCmd;
        this.sbtCmd = sbtCmd;
    }

    @Override
    public File getInstanceDir() {
        return projectRoot.getParentFile().getParentFile();
    }

    @Override
    public void start(final InvocationOutputHandler buildLogHandler,
                      final InvocationOutputHandler consoleLogHandler,
                      final Map<String, String> envVarsForApp,
                      final Waiter startupWaiter) throws ProjectCannotStartException {

        final String artifactPath = runSbt(buildLogHandler, envVarsForApp, CLEAN_AND_PACKAGE);

        final CommandLine command = javaCmd
            .commandLine(envVarsForApp)
            .addArgument("-jar")
            .addArgument(artifactPath);

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);
    }

    @Override
    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }

    private String runSbt(final InvocationOutputHandler buildLogHandler,
                          final Map<String, String> envVarsForApp,
                          final List<String> arguments) {

        final CommandLine command = sbtCmd.commandLine(envVarsForApp);
        for (final String argument : arguments) {
            command.addArgument(argument);
        }

        buildLogHandler.consumeLine("Running sbt " + StringUtils.join(arguments, " ") + " with " + command);

        final CircularFifoQueue<String> latestBuildLog = new CircularFifoQueue<>(SBT_LINE_BUFFER);

        final InvocationOutputHandler capturingBuildLogHandler = line -> {
            buildLogHandler.consumeLine(line);
            latestBuildLog.add(line);
        };

        ProcessStarter.run(capturingBuildLogHandler, envVarsForApp, command, projectRoot, MINUTES.toMillis(20));

        for (final String line : latestBuildLog) {
            final Matcher matcher = ARTIFACT_PATH_REGEX.matcher(line);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }

        throw new RuntimeException("Artifact path not found within last " + SBT_LINE_BUFFER + " lines of build log");
    }
}
