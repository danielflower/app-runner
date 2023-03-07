package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

class GoRunnerFactory implements AppRunnerFactory {

    public static final Logger log = LoggerFactory.getLogger(GoRunnerFactory.class);
    private final CommandLineProvider goCommandProvider;
    private final String versionInfo;

    public GoRunnerFactory(CommandLineProvider goCommandProvider, String versionInfo) {
        this.goCommandProvider = goCommandProvider;
        this.versionInfo = versionInfo;
    }

    @Override
    public String id() {
        return "golang";
    }

    @Override
    public String sampleProjectName() {
        return "golang.zip";
    }

    @Override
    public String description() {
        return "Go executable built with go build";
    }

    @Override
    public String[] startCommands() {
        return GoRunner.startCommands;
    }

    @Override
    public AppRunner appRunner(File folder) {
        return new GoRunner(folder, goCommandProvider);
    }

    @Override
    public String versionInfo() {
        return versionInfo;
    }

    @Override
    public boolean canRun(File appDirectory) {
        boolean canRun = false;
        for (File currentFile : appDirectory.listFiles()) {
            if (FilenameUtils.isExtension(currentFile.getName(), "go")) {
                canRun = true;
            }
        }
        return canRun;
    }

    public static Optional<GoRunnerFactory> createIfAvailable(Config config) {
        CommandLineProvider goCmdProvider = config.goCommandProvider();
        Pair<Boolean, String> version = ProcessStarter.run(goCmdProvider.commandLine(config.env()).addArgument("version"));
        if (version.getLeft()) {
            return Optional.of(new GoRunnerFactory(goCmdProvider, version.getRight()));
        }
        return Optional.empty();
    }
}
