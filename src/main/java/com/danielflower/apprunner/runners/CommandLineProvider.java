package com.danielflower.apprunner.runners;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.SystemUtils;

import java.util.Map;

public interface CommandLineProvider {
    CommandLineProvider lein_on_path = (Map<String, String> m) -> new CommandLine(SystemUtils.IS_OS_WINDOWS ? "lein.bat" : "lein");

    CommandLineProvider sbt_on_path = (Map<String, String> m) -> new CommandLine(SystemUtils.IS_OS_WINDOWS ? "sbt.bat" : "sbt");

    CommandLine commandLine(Map<String, String> envVarsForApp);
}
