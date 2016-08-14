package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Optional;

class SbtRunnerFactory implements AppRunnerFactory {

    private final CommandLineProvider sbtJavaCommandProvider;
    private final CommandLineProvider sbtCommandProvider;
    private final String versionInfo;

    SbtRunnerFactory(CommandLineProvider sbtJavaCommandProvider, CommandLineProvider sbtCommandProvider, String versionInfo) {
        this.sbtJavaCommandProvider = sbtJavaCommandProvider;
        this.sbtCommandProvider = sbtCommandProvider;
        this.versionInfo = versionInfo;
    }

    @Override
    public String id() {
        return "sbt";
    }

    @Override
    public String sampleProjectName() {
        return "sbt.zip";
    }

    @Override
    public String description() {
        return "Scala uber jars built with sbt";
    }

    @Override
    public String[] startCommands() {
        return SbtRunner.startCommands;
    }

    @Override
    public AppRunner appRunner(File folder) {
        return new SbtRunner(folder, sbtJavaCommandProvider, sbtCommandProvider);
    }

    @Override
    public String versionInfo() {
        return versionInfo;
    }

    @Override
    public boolean canRun(File appDirectory) {
        return new File(appDirectory, "build.sbt").isFile();
    }

    public static Optional<SbtRunnerFactory> createIfAvailable(Config config) {
        CommandLineProvider sbtJavaCommandProvider = config.sbtJavaCommandProvider();
        CommandLineProvider sbtCommandProvider = config.sbtCommandProvider();
        Pair<Boolean, String> version = ProcessStarter.run(sbtCommandProvider.commandLine(null).addArgument("about"));
        if (version.getLeft()) {
            return Optional.of(new SbtRunnerFactory(sbtJavaCommandProvider, sbtCommandProvider, version.getRight()));
        }
        return Optional.empty();
    }
}
