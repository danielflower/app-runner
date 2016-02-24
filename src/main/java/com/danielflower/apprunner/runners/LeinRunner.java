package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.FileSandbox;
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

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static com.danielflower.apprunner.runners.MavenRunner.loadPomModel;

public class LeinRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(LeinRunner.class);
    private final File projectRoot;
    private final File leinJar;
    private final File javaExec;
    private final File tempDir;
    private ExecuteWatchdog watchDog;

    public LeinRunner(File projectRoot, File leinJar, File javaExec, File tempDir) {
        this.projectRoot = projectRoot;
        this.leinJar = leinJar;
        this.javaExec = javaExec;
        this.tempDir = tempDir;
    }


    public void start(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {
        runLein(buildLogHandler, envVarsForApp, "do", "test,", "uberjar,", "pom");

        CommandLine command = new CommandLine(javaExec);

        Model model = loadPomModel(new File(projectRoot, "pom.xml"));
        String jarName = model.getArtifactId() + "-" + model.getVersion() + "-standalone.jar";
        command.addArgument("-jar").addArgument("target" + File.separator + jarName);

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);
    }

    public void runLein(InvocationOutputHandler buildLogHandler, Map<String, String> envVarsForApp, String... arguments) {

        CommandLine command = new CommandLine(javaExec)
            .addArgument("-cp")
            .addArgument(dirPath(leinJar))
            .addArgument("-Djava.io.tmpdir=" + dirPath(tempDir))
            .addArgument("clojure.main").addArgument("-m").addArgument("leiningen.core.main");
        for (String argument : arguments) {
            command.addArgument(argument);
        }

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

        private final File leinJar;
        private final File javaExec;
        private final FileSandbox fileSandbox;

        public Factory(File leinJar, File javaExec, FileSandbox fileSandbox) {
            this.leinJar = leinJar;
            this.javaExec = javaExec;
            this.fileSandbox = fileSandbox;
        }

        @Override
        public Optional<AppRunner> forProject(String appName, File projectRoot) {
            File projectClj = new File(projectRoot, "project.clj");
            if (projectClj.isFile()) {
                LeinRunner runner = new LeinRunner(projectRoot, leinJar, javaExec, fileSandbox.tempDir(appName));
                return Optional.of(runner);
            } else {
                return Optional.empty();
            }
        }

        public String toString() {
            return "Leiningin runner for Clojure apps using " + leinJar.getName();
        }
    }

}
