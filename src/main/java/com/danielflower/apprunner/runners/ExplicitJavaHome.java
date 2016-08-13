package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;
import java.util.Map;

import static com.danielflower.apprunner.FileSandbox.fullPath;

public class ExplicitJavaHome implements HomeProvider {
    private final File javaHome;

    public ExplicitJavaHome(File javaHome) {
        this.javaHome = javaHome;
    }

    public InvocationRequest mungeMavenInvocationRequest(InvocationRequest request) {
        return request.setJavaHome(javaHome);
    }

    public CommandLine commandLine(Map<String, String> envVarsForApp) {
        return new CommandLine(FileUtils.getFile(javaHome, "bin", Config.javaExecutableName()));
    }

    public String toString() {
        return fullPath(javaHome);
    }
}
