package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Optional;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static java.util.Arrays.asList;

public class MavenRunner implements AppRunner {
    private static final Logger log = LoggerFactory.getLogger(MavenRunner.class);
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


        File javaExec = FileUtils.getFile(javaHome, "bin", SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java");
        CommandLine command = new CommandLine(javaExec);
        command.addArgument("-jar").addArgument("target" + File.separator + jarName);

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot);
    }

    public void shutdown() {
        if (watchDog != null)
            watchDog.destroyProcess();
    }

    public static class Factory implements AppRunner.Factory {

        private final File javaHome;

        public Factory(File javaHome) {
            this.javaHome = javaHome;
        }

        @Override
        public Optional<AppRunner> forProject(String appName, File projectRoot) {
            File pom = new File(projectRoot, "pom.xml");
            if (pom.isFile()) {
                AppRunner runner = new MavenRunner(projectRoot, javaHome);
                return Optional.of(runner);
            } else {
                return Optional.empty();
            }
        }

        public String toString() {
            return "Maven builder for Java using " + dirPath(javaHome);
        }
    }

}
