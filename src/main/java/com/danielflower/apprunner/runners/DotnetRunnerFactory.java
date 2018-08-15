package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Optional;

/**
 * Created by Evan Huang on Aug 14, 2018.
 */
public class DotnetRunnerFactory implements AppRunnerFactory {
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(DotnetRunnerFactory.class);
    private final String versionString;
    private final String executable;

    DotnetRunnerFactory(String executable, String versionInfo) {
        this.executable = executable;
        this.versionString = versionInfo;
    }

    public static Optional<DotnetRunnerFactory> createIfAvailable(Config config) {

        String executable = config.dotnetExecutableName();

        Pair<Boolean, String> proc = ProcessStarter.run(new CommandLine(executable).addArgument("--version"));
        if (proc.getLeft()) {
            String versionInfo = proc.getRight();
            return Optional.of(new DotnetRunnerFactory(executable, versionInfo));
        } else {
            //log.warn(executable + " couldn't be run, so the DotnetRunnerFactory has not been created");
            return Optional.empty();
        }

    }

    @Override
    public String id() {
        return "dotnet";
    }

    @Override
    public String sampleProjectName() {
        return "dotnet.zip";
    }

    @Override
    public String description() {
        return ".NET Core apps";
    }

    @Override
    public String[] startCommands() {
        return DotnetRunner.startCommands;
    }

    @Override
    public AppRunner appRunner(File folder) {
        return new DotnetRunner(folder, getProjectFile(folder), executable);
    }

    @Override
    public String versionInfo() {
        return versionString;
    }

    @Override
    public boolean canRun(File appDirectory) {
        if (!appDirectory.isDirectory())
            return false;

        File result = getProjectFile(appDirectory);

        return result != null;    //without -p option "dotnet run" will work when only one project file found
    }

    private File getProjectFile(File appDirectory) {
        File[] projectFiles = appDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".csproj") || filename.endsWith(".vbproj");
            }
        });

        File result = null;
        if (projectFiles.length >= 1)
            result = projectFiles[0];

        return result;
    }
}
