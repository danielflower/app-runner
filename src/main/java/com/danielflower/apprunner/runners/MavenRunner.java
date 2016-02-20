package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jetty.io.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static java.util.Arrays.asList;

public class MavenRunner {
    public static final Logger log = LoggerFactory.getLogger(MavenRunner.class);
    private final File projectRoot;
    private ExecuteWatchdog watchDog;
    private final File javaHome;

    public MavenRunner(File projectRoot, File javaHome) {
        this.projectRoot = projectRoot;
        this.javaHome = javaHome;
    }

    public void start(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp) throws ProjectCannotStartException {
        File pomFile = new File(projectRoot, "pom.xml");

        InvocationRequest request = new DefaultInvocationRequest()
            .setPomFile(pomFile)
            .setJavaHome(javaHome)
            .setOutputHandler(buildLogHandler)
            .setErrorHandler(buildLogHandler)
            .setGoals(asList("clean", "package"))
            .setBaseDirectory(projectRoot);

        log.info("Building maven project at " + dirPath(projectRoot));
        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new ProjectCannotStartException("Build returned error", result.getExecutionException());
            }
        } catch (Exception e) {
            throw new ProjectCannotStartException("Error while building " + projectRoot.getAbsolutePath(), e);
        }
        log.info("Build successful. Going to start app.");

        String jarName;
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            try (FileReader pomReader = new FileReader(pomFile)) {
                Model model = reader.read(pomReader);
                jarName = model.getArtifactId() + "-" + model.getVersion() + ".jar";
            }
        } catch (Exception e) {
            throw new ProjectCannotStartException("Error while reading maven meta data", e);
        }

        File jar = new File(new File(projectRoot, "target"), jarName);
        if (!jar.isFile()) {
            throw new ProjectCannotStartException("Could not find the jar file at " + dirPath(jar));
        }

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File(projectRoot, "target"));
        watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        executor.setWatchdog(watchDog);

        executor.setStreamHandler(new PumpStreamHandler(new WriterOutputStream(new WriterToOutputBridge(consoleLogHandler))));
        File javaExec = FileUtils.getFile(javaHome, "bin", SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java");
        CommandLine command = new CommandLine(javaExec);
        command.addArgument("-jar").addArgument(jarName);
        consoleLogHandler.consumeLine(dirPath(executor.getWorkingDirectory()) + "> " + String.join(" ", command.toStrings()) + "\n");

        try {
            DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            executor.execute(command, envVarsForApp, handler);
            handler.waitFor(TimeUnit.SECONDS.toMillis(2));
            if (handler.hasResult()) {
                String message = "The project at " + dirPath(projectRoot) + " started but exited all too soon. Check the console log for information.";
                buildLogHandler.consumeLine(message);
                throw new ProjectCannotStartException(message);
            }
        } catch (Exception e) {
            String message = "Built successfully, but error on start for " + dirPath(projectRoot);
            buildLogHandler.consumeLine(message);
            buildLogHandler.consumeLine(e.toString());
            throw new ProjectCannotStartException(message, e);
        }

        log.info("Started " + jarName);
    }

    public void shutdown() {
        watchDog.destroyProcess();
    }

}
