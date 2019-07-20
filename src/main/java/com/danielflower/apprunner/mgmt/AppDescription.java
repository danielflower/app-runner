package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.runners.AppRunnerFactoryProvider;
import org.apache.maven.shared.invoker.InvocationOutputHandler;

import java.io.File;
import java.util.ArrayList;

public interface AppDescription {
    String name();

    String gitUrl();

    Availability currentAvailability();

    BuildStatus lastBuildStatus();

    BuildStatus lastSuccessfulBuild();

    String latestBuildLog();

    String latestConsoleLog();

    ArrayList<String> contributors();

    File dataDir();

    void stopApp() throws Exception;

    void update(AppRunnerFactoryProvider runnerProvider, InvocationOutputHandler outputHandler) throws Exception;

    void delete();
}
