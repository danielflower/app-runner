package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Optional;

class NodeRunnerFactory implements AppRunnerFactory {
    private final String versionInfo;
    private final String nodeExecutable;
    private final String npmExecutable;

    NodeRunnerFactory(String nodeExecutable, String npmExecutable, String versionInfo) {
        this.nodeExecutable = nodeExecutable;
        this.npmExecutable = npmExecutable;
        this.versionInfo = versionInfo;
    }

    @Override
    public AppRunner appRunner(String name, File folder) {
        return new NodeRunner(folder, nodeExecutable, npmExecutable);
    }

    @Override
    public String versionInfo() {
        return versionInfo;
    }

    @Override
    public boolean canRun(File appDirectory) {
        return new File(appDirectory, "package.json").isFile();
    }

    public static Optional<NodeRunnerFactory> createIfAvailable(Config config) {
        String nodeExecutable = config.nodeExecutable();
        String npmExecutable = config.npmExecutable();
        Pair<Boolean, String> node = ProcessStarter.run(new CommandLine(nodeExecutable).addArgument("--version"));
        Pair<Boolean, String> npm = ProcessStarter.run(new CommandLine(npmExecutable).addArgument("--version"));

        if (node.getLeft() && npm.getLeft()) {
            String versionInfo = "Node " + node.getRight() + " with NPM " + npm.getRight();
            return Optional.of(new NodeRunnerFactory(nodeExecutable, npmExecutable, versionInfo));
        }
        return Optional.empty();
    }
}
