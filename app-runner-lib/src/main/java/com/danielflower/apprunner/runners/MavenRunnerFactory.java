package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import io.muserver.Mutils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import static com.danielflower.apprunner.runners.MavenRunner.CLEAN_AND_PACKAGE;

public class MavenRunnerFactory implements AppRunnerFactory {
    public static final Logger log = LoggerFactory.getLogger(MavenRunnerFactory.class);

    private final HomeProvider javaHomeProvider;
    private final String versionInfo;
    private final File m2Home;

    public MavenRunnerFactory(HomeProvider javaHomeProvider, String versionInfo, File m2Home) {
        this.javaHomeProvider = javaHomeProvider;
        this.versionInfo = versionInfo;
        this.m2Home = m2Home;
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
        return new MavenRunner(m2Home, folder, folder, javaHomeProvider, CLEAN_AND_PACKAGE);
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

        File m2Home;
        try {
            m2Home = config.getDir(Config.M2_HOME);
            log.info("M2_HOME is " + Mutils.fullPath(m2Home));
        } catch (Exception e){
            log.info("M2_HOME not set");
            m2Home = null;
        }

        StringBuffer out = new StringBuffer();
        InvocationRequest request = new DefaultInvocationRequest()
            .setOutputHandler((str) -> out.append(str).append(" - "))
            .setErrorHandler((str) -> out.append(str).append(" - "))
            .setShowVersion(true)
            .setMavenHome(m2Home)
            .setGoals(Collections.singletonList("--version"))
            .setBaseDirectory(new File("."));
        try {
            MavenRunner.runRequest(request, homeProvider, m2Home);
            String versionInfo = StringUtils.removeEndIgnoreCase(out.toString(), " - ");
            return Optional.of(new MavenRunnerFactory(homeProvider, versionInfo, m2Home));
        } catch (Exception e) {
            log.info("Maven runner not available", e);
            return Optional.empty();
        }
    }
}
