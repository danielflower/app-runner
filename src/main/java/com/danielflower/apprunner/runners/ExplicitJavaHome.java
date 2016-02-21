package com.danielflower.apprunner.runners;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class ExplicitJavaHome implements JavaHomeProvider {
    private final File javaHome;

    public ExplicitJavaHome(File javaHome) {
        this.javaHome = javaHome;
    }

    public InvocationRequest mungeMavenInvocationRequest(InvocationRequest request) {
        return request.setJavaHome(javaHome);
    }

    public CommandLine javaCommandLine() {
        return new CommandLine(FileUtils.getFile(javaHome, "bin", SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java"));
    }

    public String toString() {
        return dirPath(javaHome);
    }
}
