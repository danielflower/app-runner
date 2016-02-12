package com.danielflower.laprunner.runners;

import com.danielflower.laprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jetty.io.WriterOutputStream;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.danielflower.laprunner.FileUtils.dirPath;
import static java.util.Arrays.asList;

public class MavenRunner {
    private final File projectRoot;
    private ExecuteWatchdog watchDog;
    private StringBuilder output;

    public MavenRunner(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void start(int port) throws ProjectCannotStartException {
        File pomFile = new File(projectRoot, "pom.xml");
        File javaHome = new File(System.getenv("JAVA_HOME"));
        InvocationRequest request = new DefaultInvocationRequest()
            .setPomFile(pomFile)
            .setJavaHome(javaHome)
            .setGoals(asList("clean", "package"))
            .setBaseDirectory(projectRoot)
            .addShellEnvironment("web.port", String.valueOf(port));

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new ProjectCannotStartException("Build returned error", result.getExecutionException());
            }
        } catch (Exception e) {
            throw new ProjectCannotStartException("Error while building " + projectRoot.getAbsolutePath(), e);
        }

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
        env.put("web.port", String.valueOf(port));
        try {
            DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            executor.execute(command, env, handler);
            handler.waitFor(TimeUnit.SECONDS.toMillis(5));
            if (handler.hasResult()) {
                throw new ProjectCannotStartException("The project at " + dirPath(projectRoot) + " started but exited all too soon. Output was: " + output);
            }
        } catch (Exception e) {
            throw new ProjectCannotStartException("Built successfully, but error on start for " + dirPath(projectRoot), e);
        }

    }

    public void shutdown() {
        watchDog.destroyProcess();
    }
}
