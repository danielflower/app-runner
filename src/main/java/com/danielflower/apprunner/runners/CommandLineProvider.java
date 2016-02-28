package com.danielflower.apprunner.runners;

import org.apache.commons.exec.CommandLine;

import java.util.Map;

public interface CommandLineProvider {
    CommandLineProvider lein_on_path = (Map<String, String> m) -> new CommandLine("lein");

    CommandLine commandLine(Map<String, String> envVarsForApp);
}
