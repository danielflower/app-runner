package com.danielflower.apprunner.runners;

import java.io.File;

public interface AppRunnerFactory {
    AppRunner appRunner(File folder);
    String versionInfo();
    boolean canRun(File appDirectory);
}
