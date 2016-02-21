package com.danielflower.apprunner.runners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RunnerProvider {
    public static final Logger log = LoggerFactory.getLogger(RunnerProvider.class);

    private final List<AppRunner.Factory> factories;

    public RunnerProvider(List<AppRunner.Factory> factories) {
        this.factories = factories;
    }

    public AppRunner runnerFor(String appName, File projectRoot) throws UnsupportedProjectTypeException {
        for (AppRunner.Factory factory : factories) {
            Optional<AppRunner> appRunner = factory.forProject(appName, projectRoot);
            if (appRunner.isPresent()) {
                AppRunner actual = appRunner.get();
                log.info("Using " + actual.getClass().getSimpleName() + " for " + appName);
                return actual;
            }
        }
        throw new UnsupportedProjectTypeException("No app runner found for " + projectRoot.getName());
    }

    public static RunnerProvider empty() {
        return new RunnerProvider(new ArrayList<>());
    }

}
