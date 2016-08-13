package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.WriterToOutputBridge;
import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.eclipse.jetty.io.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.danielflower.apprunner.FileSandbox.fullPath;

public class ProcessStarter {
    public static final Logger log = LoggerFactory.getLogger(ProcessStarter.class);

    public static Killer startDaemon(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp, CommandLine command, File projectRoot, Waiter startupWaiter) {
        long startTime = logStartInfo(command, projectRoot);
        Killer watchDog = new Killer(ExecuteWatchdog.INFINITE_TIMEOUT);
        Executor executor = createExecutor(consoleLogHandler, command, projectRoot, watchDog);

        try {
            DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            executor.execute(command, envVarsForApp, handler);

            startupWaiter.or(c -> handler.hasResult()); // stop waiting if the process exist
            startupWaiter.blockUntilReady();

            if (handler.hasResult()) {
                String message = "The project at " + fullPath(projectRoot) + " started but exited all too soon. Check the console log for information.";
                buildLogHandler.consumeLine(message);
                throw new ProjectCannotStartException(message);
            }
        } catch (TimeoutException te) {
            String message = "Built successfully, but timed out waiting for startup at " + fullPath(projectRoot);
            watchDog.destroyProcess();
            buildLogHandler.consumeLine(message);
            throw new ProjectCannotStartException(message);
        } catch (ProjectCannotStartException pcse) {
            throw pcse;
        } catch (Exception e) {
            String message = "Built successfully, but error on start for " + fullPath(projectRoot);
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
            String message = "Error running: " + fullPath(projectRoot) + "> " + StringUtils.join(command.toStrings(), " ");
            outputHandler.consumeLine(message);
            outputHandler.consumeLine(e.toString());
            throw new ProjectCannotStartException(message, e);
        }
        logEndTime(command, startTime);
    }

    public static Pair<Boolean, String> run(CommandLine command) {
        ExecuteWatchdog watchDog = new ExecuteWatchdog(30000);
        StringBuffer output = new StringBuffer();
        Executor executor = createExecutor(output::append, command, new File("."), watchDog);
        output.setLength(0);
        try {
            int exitValue = executor.execute(command);
            if (executor.isFailure(exitValue)) {
                return Pair.of(false, "Not available");
            }
        } catch (Exception e) {
            return Pair.of(false, "Not available");
        }
        return Pair.of(true, output.toString().trim());
    }

    public static long logStartInfo(CommandLine command, File projectRoot) {
        log.info("Starting " + fullPath(projectRoot) + "> " + StringUtils.join(command.toStrings(), " "));
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
        consoleLogHandler.consumeLine(fullPath(executor.getWorkingDirectory()) + "> " + String.join(" ", command.toStrings()) + "\n");
        return executor;
    }

}
