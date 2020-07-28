package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Optional;

class RustRunnerFactory implements AppRunnerFactory {
    private final String versionInfo;
    private final String cargoExecutable;
    private final boolean removeBuildFiles;

    RustRunnerFactory(String cargoExecutable, String versionInfo, boolean removeBuildFiles) {
        this.cargoExecutable = cargoExecutable;
        this.versionInfo = versionInfo;
        this.removeBuildFiles = removeBuildFiles;
    }

    @Override
    public String id() {
        return "rust";
    }

    @Override
    public String sampleProjectName() {
        return "rust.zip";
    }

    @Override
    public String description() {
        return "Rust apps with cargo";
    }

    @Override
    public String[] startCommands() {
        return RustRunner.startCommands;
    }

    @Override
    public AppRunner appRunner(File folder) {
        return new RustRunner(folder, cargoExecutable, removeBuildFiles);
    }

    @Override
    public String versionInfo() {
        return versionInfo;
    }

    @Override
    public boolean canRun(File appDirectory) {
        return new File(appDirectory, "Cargo.toml").isFile();
    }

    public static Optional<RustRunnerFactory> createIfAvailable(Config config) {
        String cargoExecutable = config.cargoExecutable();
        boolean removeBuildFiles = config.cargoRemoveBuildFiles();

        Pair<Boolean, String> cargoVersion = ProcessStarter.run(new CommandLine(cargoExecutable).addArgument("--version"));

        if (cargoVersion.getLeft()) {
            return Optional.of(new RustRunnerFactory(cargoExecutable, cargoVersion.getRight(), removeBuildFiles));
        }

        return Optional.empty();
    }
}
