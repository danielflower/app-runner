package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.runners.RunnerProvider;
import org.apache.maven.shared.invoker.InvocationOutputHandler;

public interface AppDescription {
    String name();

    String gitUrl();

    String latestBuildLog();

    String latestConsoleLog();

    void stopApp() throws Exception;

    void update(RunnerProvider runnerProvider, InvocationOutputHandler outputHandler) throws Exception;
}
