package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.LineConsumer;
import com.danielflower.apprunner.problems.ProjectCannotStartException;

import java.io.File;
import java.util.Map;

public interface AppRunner {
    void start(LineConsumer buildLogHandler, LineConsumer consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException;

    void shutdown();

    File getInstanceDir();
}
