package com.danielflower.apprunner.runners;

import org.apache.commons.exec.CommandLine;
import org.apache.maven.shared.invoker.InvocationRequest;

public interface JavaHomeProvider extends JavaCommandLineProvider {
    JavaHomeProvider default_java_home = new JavaHomeProvider() {
        public InvocationRequest mungeMavenInvocationRequest(InvocationRequest request) { return request; }

        public CommandLine javaCommandLine() { return new CommandLine("java"); }
    };

    InvocationRequest mungeMavenInvocationRequest(InvocationRequest request);
}
