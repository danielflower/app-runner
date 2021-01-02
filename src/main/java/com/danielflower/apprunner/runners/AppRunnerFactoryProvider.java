package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static java.util.stream.Collectors.joining;

public class AppRunnerFactoryProvider {
    public static final Logger log = LoggerFactory.getLogger(AppRunnerFactoryProvider.class);

    private final List<AppRunnerFactory> factories;

    public AppRunnerFactoryProvider(List<AppRunnerFactory> factories) {
        this.factories = factories;
    }

    public static AppRunnerFactoryProvider create(Config config) throws ExecutionException, InterruptedException {
        // This is done asyncronously because around half the startup time in tests was due to these calls.

        ExecutorService executorService = Executors.newFixedThreadPool(6 /* the number of factories */);
        List<Future<Optional<? extends AppRunnerFactory>>> futures = new ArrayList<>();
        futures.add(executorService.submit(() -> MavenRunnerFactory.createIfAvailable(config)));
        futures.add(executorService.submit(() -> NodeRunnerFactory.createIfAvailable(config)));
        futures.add(executorService.submit(() -> LeinRunnerFactory.createIfAvailable(config)));
        futures.add(executorService.submit(() -> SbtRunnerFactory.createIfAvailable(config)));
        futures.add(executorService.submit(() -> GoRunnerFactory.createIfAvailable(config)));
        futures.add(executorService.submit(() -> GradleRunnerFactory.createIfAvailable(config)));
        futures.add(executorService.submit(() -> PythonRunnerFactory.createIfAvailable(config, 2)));
        futures.add(executorService.submit(() -> PythonRunnerFactory.createIfAvailable(config, 3)));
        futures.add(executorService.submit(() -> RustRunnerFactory.createIfAvailable(config)));
        futures.add(executorService.submit(() -> DotnetRunnerFactory.createIfAvailable(config)));

        List<AppRunnerFactory> factories = new ArrayList<>();
        for (Future<Optional<? extends AppRunnerFactory>> future : futures) {
            future.get().ifPresent(factories::add);
        }
        executorService.shutdown();
        return new AppRunnerFactoryProvider(factories);
    }

    public AppRunnerFactory runnerFor(String appName, File projectRoot) throws UnsupportedProjectTypeException {
        for (AppRunnerFactory factory : factories) {
            if (factory.canRun(projectRoot)) {
                return factory;
            }
        }
        throw new UnsupportedProjectTypeException("No app runner found for " + appName);
    }

    public String describeRunners() {
        return factories.stream()
                .map(AppRunnerFactory::versionInfo)
                .collect(joining(System.lineSeparator()));
    }

    public List<AppRunnerFactory> factories() {
        return factories;
    }
}
