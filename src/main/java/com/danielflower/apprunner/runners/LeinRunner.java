package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.FileSandbox;
import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.danielflower.apprunner.FileSandbox.dirPath;

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


    public void start(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp) throws ProjectCannotStartException {
        runLein(buildLogHandler, envVarsForApp, "test");
        runLein(buildLogHandler, envVarsForApp, "uberjar");

        CommandLine command = new CommandLine(javaExec);


        Properties pom = loadBuildProperties(projectRoot);
        String artifactId = pom.getProperty("artifactId");
        String version = pom.getProperty("version");

        String jarName = artifactId + "-" + version + "-standalone.jar";
        command.addArgument("-jar").addArgument("target" + File.separator + jarName);

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot);
    }

    public static Properties loadBuildProperties(File projectRoot) {
        File mavenMetaInfoDir = new File(projectRoot, FilenameUtils.separatorsToSystem("target/classes/META-INF/maven/"));
        try {
            File pomProperties = FileUtils.iterateFiles(
                mavenMetaInfoDir, new NameFileFilter("pom.properties"), TrueFileFilter.INSTANCE).next();
            Properties pom = new Properties();
            try (Reader reader = new FileReader(pomProperties)) {
                pom.load(reader);
            }
            return pom;
        } catch (Exception e) {
            throw new ProjectCannotStartException("Could not find pom.properties anywhere under " + dirPath(mavenMetaInfoDir));
        }
    }

    public void runLein(InvocationOutputHandler buildLogHandler, Map<String, String> envVarsForApp, String argument) {

        CommandLine command = new CommandLine(javaExec)
            .addArgument("-cp")
            .addArgument(dirPath(leinJar))
            .addArgument("-Djava.io.tmpdir=" + dirPath(tempDir))
            .addArgument("clojure.main").addArgument("-m").addArgument("leiningen.core.main")
            .addArgument(argument);

        buildLogHandler.consumeLine("Running lein " + argument + " with " + command);
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
