package com.danielflower.apprunner.runners;

import java.io.File;

public interface AppRunnerFactory {
    AppRunner appRunner(String name, File folder);
    String versionInfo();
    boolean canRun(File appDirectory);
}
