package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.maven.shared.invoker.InvocationOutputHandler;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public interface AppRunner {
    void start(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException;

    void shutdown();

    interface Factory {

        Optional<AppRunner> forProject(String appName, File projectRoot);

    }

}
