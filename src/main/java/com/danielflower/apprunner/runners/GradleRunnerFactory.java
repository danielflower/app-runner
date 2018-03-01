package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Optional;

public class GradleRunnerFactory implements AppRunnerFactory {

    private String versionInfo;
    private String gradleExec;
    private HomeProvider javaHomeProvider;

    public GradleRunnerFactory(String gradleExec, HomeProvider javaHomeProvider, String versionInfo) {
        this.javaHomeProvider = javaHomeProvider;
        this.versionInfo = versionInfo;
        this.gradleExec = gradleExec;
    }

    public static Optional<GradleRunnerFactory> createIfAvailable(Config config) {
        String gradleExec = config.gradleExecutableName();
        HomeProvider javaHomeProvider = config.javaHomeProvider();

        Pair<Boolean, String> gradle = ProcessStarter.run(new CommandLine(gradleExec).addArgument("-v"));
        Pair<Boolean, String> java = ProcessStarter.run(javaHomeProvider.commandLine(null).addArgument("-version"));

        if (gradle.getLeft() && java.getLeft()) {
            String versionInfo = "Gradle " + gradle.getRight() + " with Java " + java.getRight();
            return Optional.of(new GradleRunnerFactory(gradleExec, javaHomeProvider, versionInfo));
        }

        return Optional.empty();
    }

    @Override
    public String id() {
        return "gradle";
    }

    @Override
    public String sampleProjectName() {
        return "gradle.zip";
    }

    @Override
    public String description() {
        return "Java uber jars built with Gradle";
    }

    @Override
    public String[] startCommands() {
        return GradleRunner.startCommands;
    }

    @Override
    public AppRunner appRunner(File folder) {
        return new GradleRunner(folder, gradleExec, javaHomeProvider);
    }

    @Override
    public String versionInfo() {
        return versionInfo;
    }

    @Override
    public boolean canRun(File appDirectory) {
        File build_gradle = new File(appDirectory, "build.gradle");
        return build_gradle.isFile();
    }
}
