package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.runners.RunnerProvider;
import org.apache.maven.shared.invoker.InvocationOutputHandler;

import java.util.ArrayList;

public interface AppDescription {
    String name();

    String gitUrl();

    Availability currentAvailability();

    String latestBuildLog();

    String latestConsoleLog();

    ArrayList<String> contributors();

    void stopApp() throws Exception;

    void update(RunnerProvider runnerProvider, InvocationOutputHandler outputHandler) throws Exception;
}
