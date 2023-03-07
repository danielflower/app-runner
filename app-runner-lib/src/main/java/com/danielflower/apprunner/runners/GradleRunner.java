package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.LineConsumer;
import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.danielflower.apprunner.FileSandbox.fullPath;

public class GradleRunner implements AppRunner {
    public static final long DEFAULT_GRADLE_CLEAN_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    public static final Logger log = LoggerFactory.getLogger(GradleRunner.class);
    public static final String[] startCommands = new String[]{
        "gradle clean",
        "gradle shadowJar",
        "java -jar build/libs/{artifactid}-{version}-all.jar"
    };

    private File projectRoot;
    private String gradleExec;
    private HomeProvider javaHomeProvider;
    private ExecuteWatchdog watchDog;

    public GradleRunner(File projectRoot, String gradleExec, HomeProvider javaHomeProvider) {
        this.projectRoot = projectRoot;
        this.gradleExec = gradleExec;
        this.javaHomeProvider = javaHomeProvider;
    }

    @Override
    public void start(LineConsumer buildLogHandler, LineConsumer consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {
        gradleClean(buildLogHandler, envVarsForApp);
        gradleBuild(buildLogHandler, envVarsForApp);
        watchDog = runJar(buildLogHandler, consoleLogHandler, envVarsForApp, startupWaiter);
    }

    @Override
    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }

    private void gradleClean(LineConsumer buildLogHandler, Map<String, String> envVarsForApp) {
        CommandLine command = new CommandLine(gradleExec).addArgument("clean");
        buildLogHandler.consumeLine("Running gradle clean");
        ProcessStarter.run(buildLogHandler, envVarsForApp, command, projectRoot, DEFAULT_GRADLE_CLEAN_TIMEOUT);
    }

    private void gradleBuild(LineConsumer buildLogHandler, Map<String, String> envVarsForApp) {
        CommandLine command = new CommandLine(gradleExec).addArgument("shadowJar");
        buildLogHandler.consumeLine("Running gradle shadowJar");
        ProcessStarter.run(buildLogHandler, envVarsForApp, command, projectRoot, DEFAULT_GRADLE_CLEAN_TIMEOUT);
    }

    private ExecuteWatchdog runJar(LineConsumer buildLogHandler, LineConsumer consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) {
        Path libsPath = Paths.get(projectRoot.getPath(), "build", "libs");
        File libsFolder = libsPath.toFile();

        // To simplify implementation, now I assume only 1 uberjar named "artifact-version-all.jar" under libs folder
        // As we clean the project every time, I can't foresee any possibility that will mix up other uberjars.
        File[] files = libsFolder.listFiles();

        if(files == null) {
            throw new ProjectCannotStartException(libsFolder.getPath() + " doesn't exist");
        }

        Optional<File> jar = Stream.of(files).filter((f) -> f.getName().contains("all")).findFirst();

        if (!jar.isPresent() || !jar.get().isFile()) {
            throw new ProjectCannotStartException("Could not find the jar file at " + jar.get().getPath());
        }

        CommandLine command = javaHomeProvider.commandLine(envVarsForApp)
            .addArgument("-Djava.io.tmpdir=" + envVarsForApp.get("TEMP"))
            .addArgument("-jar")
            .addArgument(fullPath(jar.get()));

        return ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);
    }

    @Override
    public File getInstanceDir() {
        return null;
    }
}
