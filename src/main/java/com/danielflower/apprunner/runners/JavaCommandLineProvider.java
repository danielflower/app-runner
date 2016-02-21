package com.danielflower.apprunner.runners;

import org.apache.commons.exec.CommandLine;

public interface JavaCommandLineProvider {
    CommandLine javaCommandLine();
}
