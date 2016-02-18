package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.eclipse.jetty.io.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static java.util.Arrays.asList;

public class MavenRunner {
    public static final Logger log = LoggerFactory.getLogger(MavenRunner.class);
    private final File projectRoot;
    private ExecuteWatchdog watchDog;
    private StringBuilder output;
    private final File javaHome;

    public MavenRunner(File projectRoot, File javaHome) {
        this.projectRoot = projectRoot;
        this.javaHome = javaHome;
    }

    public void start(int port, InvocationOutputHandler outputHandler, String name) throws ProjectCannotStartException {
        File pomFile = new File(projectRoot, "pom.xml");

        InvocationRequest request = new DefaultInvocationRequest()
            .setPomFile(pomFile)
            .setJavaHome(javaHome)
            .setOutputHandler(outputHandler)
            .setErrorHandler(outputHandler)
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

        output = new StringBuilder();
        executor.setStreamHandler(new PumpStreamHandler(new WriterOutputStream(new StringBuilderWriter(output))));
        File javaExec = FileUtils.getFile(javaHome, "bin", SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java");
        CommandLine command = new CommandLine(javaExec);
        command.addArgument("-jar").addArgument(jarName);
        Map<String, String> env = new HashMap<>();
        env.put("APP_PORT", String.valueOf(port));
        env.put("APP_NAME", name);
//        try {
            outputHandler.consumeLine("Running: " + String.join(" ", command.toStrings()));
//            writer.write("Running: " + String.join(" ", command.toStrings()) + "\n");
//        } catch (IOException e) {
//            log.info("Error while writing", e);
//        }
        try {
            DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            executor.execute(command, env, handler);
            handler.waitFor(TimeUnit.SECONDS.toMillis(2));
            if (handler.hasResult()) {
                throw new ProjectCannotStartException("The project at " + dirPath(projectRoot) + " started but exited all too soon. Output was: " + output);
            }
        } catch (Exception e) {
            throw new ProjectCannotStartException("Built successfully, but error on start for " + dirPath(projectRoot), e);
        }

        outputHandler.consumeLine("Current output from app:\n");
        outputHandler.consumeLine(output.toString() + "\n");
/*
        try {
            writer.write("Current output from app:\n");
            writer.write(output.toString() + "\n");
        } catch (IOException e) {
            log.info("Error while writing to output", e);
        }
*/

        log.info("Started " + jarName);
    }

    public void shutdown() {
        watchDog.destroyProcess();
    }
}
