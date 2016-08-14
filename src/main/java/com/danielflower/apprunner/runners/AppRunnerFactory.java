package com.danielflower.apprunner.runners;

import java.io.File;

public interface AppRunnerFactory {
    String id();
    String sampleProjectName();
    String description();
    String[] startCommands();
    AppRunner appRunner(File folder);
    String versionInfo();
    boolean canRun(File appDirectory);
}
