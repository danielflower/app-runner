package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import static com.danielflower.apprunner.runners.MavenRunner.CLEAN_AND_PACKAGE;

public class MavenRunnerFactory implements AppRunnerFactory {

    private final HomeProvider javaHomeProvider;
    private final String versionInfo;

    public MavenRunnerFactory(HomeProvider javaHomeProvider, String versionInfo) {
        this.javaHomeProvider = javaHomeProvider;
        this.versionInfo = versionInfo;
    }

    @Override
    public String id() {
        return "maven";
    }

    @Override
    public String sampleProjectName() {
        return "maven.zip";
    }

    @Override
    public String description() {
        return "Java uber jars built with maven";
    }

    @Override
    public String[] startCommands() {
        return MavenRunner.startCommands;
    }

    @Override
    public AppRunner appRunner(File folder) {
        return new MavenRunner(folder, javaHomeProvider, CLEAN_AND_PACKAGE);
    }

    @Override
    public String versionInfo() {
        return versionInfo;
    }

    @Override
    public boolean canRun(File appDirectory) {
        return new File(appDirectory, "pom.xml").isFile();
    }

    public static Optional<MavenRunnerFactory> createIfAvailable(Config config) {
        HomeProvider homeProvider = config.javaHomeProvider();

        StringBuffer out = new StringBuffer();
        InvocationRequest request = new DefaultInvocationRequest()
            .setOutputHandler((str) -> out.append(str).append(" - "))
            .setErrorHandler((str) -> out.append(str).append(" - "))
            .setShowVersion(true)
            .setGoals(Collections.singletonList("--version"))
            .setBaseDirectory(new File("."));
        try {
            MavenRunner.runRequest(request, homeProvider);
            String versionInfo = StringUtils.removeEndIgnoreCase(out.toString(), " - ");
            return Optional.of(new MavenRunnerFactory(homeProvider, versionInfo));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
