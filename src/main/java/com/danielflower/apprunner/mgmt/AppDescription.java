package com.danielflower.apprunner.mgmt;

import org.apache.maven.shared.invoker.InvocationOutputHandler;

public interface AppDescription {
    String name();

    String gitUrl();

    String latestBuildLog();

    String latestConsoleLog();

    void stopApp() throws Exception;

    void update(InvocationOutputHandler outputHandler) throws Exception;
}
