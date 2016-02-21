package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.eclipse.jetty.io.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class ProcessStarter {
    public static final Logger log = LoggerFactory.getLogger(ProcessStarter.class);

    public static ExecuteWatchdog startDaemon(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp, CommandLine command, File projectRoot) {
        long startTime = logStartInfo(command, projectRoot);
        ExecuteWatchdog watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        Executor executor = createExecutor(consoleLogHandler, command, projectRoot, watchDog);

        try {
            DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            executor.execute(command, envVarsForApp, handler);

            handler.waitFor(TimeUnit.SECONDS.toMillis(10));
            if (handler.hasResult()) {
                String message = "The project at " + dirPath(projectRoot) + " started but exited all too soon. Check the console log for information.";
                buildLogHandler.consumeLine(message);
                throw new ProjectCannotStartException(message);
            }
        } catch (Exception e) {
            if (e instanceof ProjectCannotStartException) {
                throw (ProjectCannotStartException) e;
            }
            String message = "Built successfully, but error on start for " + dirPath(projectRoot);
            buildLogHandler.consumeLine(message);
            buildLogHandler.consumeLine(e.toString());
            throw new ProjectCannotStartException(message, e);
        }

        logEndTime(command, startTime);
        return watchDog;
    }

    public static void run(InvocationOutputHandler outputHandler, Map<String, String> envVarsForApp, CommandLine command, File projectRoot, long timeout) throws ProjectCannotStartException {
        long startTime = logStartInfo(command, projectRoot);
        ExecuteWatchdog watchDog = new ExecuteWatchdog(timeout);
        Executor executor = createExecutor(outputHandler, command, projectRoot, watchDog);
        try {
            int exitValue = executor.execute(command, envVarsForApp);
            if (executor.isFailure(exitValue)) {
                String message = watchDog.killedProcess()
                    ? "Timed out waiting for " + command
                    : "Exit code " + exitValue + " returned from " + command;
                throw new ProjectCannotStartException(message);
            }
        } catch (Exception e) {
            String message = "Error running: " + dirPath(projectRoot) + "> " + StringUtils.join(command.toStrings(), " ");
            outputHandler.consumeLine(message);
            outputHandler.consumeLine(e.toString());
            throw new ProjectCannotStartException(message, e);
        }
        logEndTime(command, startTime);
    }

    public static long logStartInfo(CommandLine command, File projectRoot) {
        log.info("Starting " + dirPath(projectRoot) + "> " + StringUtils.join(command.toStrings(), " "));
        return System.currentTimeMillis();
    }

    private static void logEndTime(CommandLine command, long startTime) {
        log.info("Completed " + command.getExecutable() + " in " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private static Executor createExecutor(InvocationOutputHandler consoleLogHandler, CommandLine command, File projectRoot, ExecuteWatchdog watchDog) {
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(projectRoot);
        executor.setWatchdog(watchDog);
        executor.setStreamHandler(new PumpStreamHandler(new WriterOutputStream(new WriterToOutputBridge(consoleLogHandler))));
        consoleLogHandler.consumeLine(dirPath(executor.getWorkingDirectory()) + "> " + String.join(" ", command.toStrings()) + "\n");
        return executor;
    }

}
