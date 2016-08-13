package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class AppRunnerFactoryProvider {
    public static final Logger log = LoggerFactory.getLogger(AppRunnerFactoryProvider.class);

    private final List<AppRunnerFactory> factories;

    public AppRunnerFactoryProvider(List<AppRunnerFactory> factories) {
        this.factories = factories;
    }

    public static AppRunnerFactoryProvider create(Config config) {
        List<AppRunnerFactory> factories = new ArrayList<>();
        MavenRunnerFactory.createIfAvailable(config).ifPresent(factories::add);
        NodeRunnerFactory.createIfAvailable(config).ifPresent(factories::add);
        LeinRunnerFactory.createIfAvailable(config).ifPresent(factories::add);
        SbtRunnerFactory.createIfAvailable(config).ifPresent(factories::add);
        return new AppRunnerFactoryProvider(factories);
    }

    public AppRunner runnerFor(String appName, File projectRoot) throws UnsupportedProjectTypeException {

        for (AppRunnerFactory factory : factories) {
            if (factory.canRun(projectRoot)) {
                AppRunner runner = factory.appRunner(appName, projectRoot);
                log.info("Using " + runner.getClass().getSimpleName() + " for " + appName);
                return runner;
            }
        }

        throw new UnsupportedProjectTypeException("No app runner found for " + appName);
    }

    public String describeRunners() {
        return factories.stream()
                .map(AppRunnerFactory::versionInfo)
                .collect(joining("\n"));
    }

}
