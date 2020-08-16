package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.runners.AppRunnerFactoryProvider;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

public interface AppDescription {
    String name();

    String gitUrl();

    void gitUrl(String url) throws URISyntaxException, GitAPIException;

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
