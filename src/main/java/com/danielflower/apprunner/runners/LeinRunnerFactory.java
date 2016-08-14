package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Optional;

class LeinRunnerFactory implements AppRunnerFactory {

    private final CommandLineProvider leinCmdProvider;
    private final CommandLineProvider leinJavaCommandProvider;
    private final String versionInfo;

    public LeinRunnerFactory(CommandLineProvider leinCmdProvider, CommandLineProvider leinJavaCommandProvider, String versionInfo) {
        this.leinCmdProvider = leinCmdProvider;
        this.leinJavaCommandProvider = leinJavaCommandProvider;
        this.versionInfo = versionInfo;
    }

    @Override
    public String id() {
        return "lein";
    }

    @Override
    public String sampleProjectName() {
        return "lein.zip";
    }

    @Override
    public String description() {
        return "Clojure uber jars built with leiningen";
    }

    @Override
    public String[] startCommands() {
        return LeinRunner.startCommands;
    }

    @Override
    public AppRunner appRunner(File folder) {
        return new LeinRunner(folder, leinJavaCommandProvider, leinCmdProvider);
    }

    @Override
    public String versionInfo() {
        return versionInfo;
    }

    @Override
    public boolean canRun(File appDirectory) {
        return new File(appDirectory, "project.clj").isFile();
    }

    public static Optional<LeinRunnerFactory> createIfAvailable(Config config) {
        CommandLineProvider leinCmdProvider = config.leinCommandProvider();
        Pair<Boolean, String> version = ProcessStarter.run(leinCmdProvider.commandLine(null).addArgument("--version"));
        if (version.getLeft()) {
            return Optional.of(new LeinRunnerFactory(leinCmdProvider, config.leinJavaCommandProvider(), version.getRight()));
        }
        return Optional.empty();
    }
}
