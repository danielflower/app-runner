package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.exec.CommandLine;
import org.apache.maven.shared.invoker.InvocationRequest;

import javax.annotation.Nullable;
import java.util.Map;

public interface HomeProvider extends CommandLineProvider {
    HomeProvider default_java_home = new HomeProvider() {
        public InvocationRequest mungeMavenInvocationRequest(InvocationRequest request) { return request; }

        public CommandLine commandLine(Map<String, String> envVarsForApp) { return new CommandLine(Config.javaExecutableName()); }
    };

    InvocationRequest mungeMavenInvocationRequest(InvocationRequest request);
}
