package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.LineConsumer;
import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static java.util.Arrays.asList;

public class MavenRunner implements AppRunner {
    private static final Logger log = LoggerFactory.getLogger(MavenRunner.class);
    static final List<String> CLEAN_AND_PACKAGE = asList("clean", "package");
    public static final String[] startCommands = new String[] { "mvn clean package", "java -jar target/{artifactid}-{version}.jar" };
    private final File m2Home;

    static Model loadPomModel(File pomFile) {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            try (FileReader pomReader = new FileReader(pomFile)) {
                return reader.read(pomReader);
            }
        } catch (Exception e) {
            throw new ProjectCannotStartException("Error while reading maven meta data", e);
        }
    }

    private final File projectRoot;
    private final File uberjarRoot;
    private final HomeProvider javaHomeProvider;
    private final List<String> goals;
    private ExecuteWatchdog watchDog;

    public MavenRunner(File m2Home, File projectRoot, File uberjarRoot, HomeProvider javaHomeProvider, List<String> goals) {
        this.m2Home = m2Home;
        this.projectRoot = projectRoot;
        this.uberjarRoot = uberjarRoot;
        this.javaHomeProvider = javaHomeProvider;
        this.goals = goals;
    }

    public void start(LineConsumer buildLogHandler, LineConsumer consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {
        File projectPomFile = new File(projectRoot, "pom.xml");
        File uberJarPomFile = new File(uberjarRoot, "pom.xml");

        if (goals.isEmpty()) {
            log.info("No goals. Skipping maven build");

        } else {
            InvocationRequest request = new DefaultInvocationRequest()
                .setMavenHome(m2Home)
                .setBatchMode(true)
                .setPomFile(projectPomFile)
                .setOutputHandler(buildLogHandler::consumeLine)
                .setErrorHandler(buildLogHandler::consumeLine)
                .setGoals(goals)
                .setBaseDirectory(projectRoot);


            log.info("Building maven project at " + fullPath(projectRoot));
            runRequest(request, javaHomeProvider, m2Home);
            log.info("Build successful. Going to start app.");
        }

        Model model = loadPomModel(uberJarPomFile);
        String jarName = model.getArtifactId() + "-" + model.getVersion() + ".jar";

        File jar = new File(new File(uberjarRoot, "target"), jarName);
        if (!jar.isFile()) {
            throw new ProjectCannotStartException("Could not find the jar file at " + fullPath(jar));
        }

        CommandLine command = javaHomeProvider.commandLine(envVarsForApp)
            .addArgument("-Djava.io.tmpdir=" + envVarsForApp.get("TEMP"))
            .addArgument("-jar")
            .addArgument("target" + File.separator + jarName);

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, uberjarRoot, startupWaiter);
    }

    static void runRequest(InvocationRequest request, HomeProvider javaHomeProvider, File m2Home1) {
        request = javaHomeProvider.mungeMavenInvocationRequest(request);
        request.setBatchMode(true);
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(m2Home1);
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new ProjectCannotStartException("Build returned error", result.getExecutionException());
            }
        } catch (Exception e) {
            throw new ProjectCannotStartException("Error while building " + fullPath(request.getBaseDirectory()), e);
        }
    }

    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        } else {
            log.info("Shutdown requested but watchdog is null");
        }
    }

    @Override
    public File getInstanceDir() {
        return projectRoot;
    }

}
