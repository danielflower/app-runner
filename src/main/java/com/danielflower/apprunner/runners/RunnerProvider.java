package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.danielflower.apprunner.runners.MavenRunner.CLEAN_AND_PACKAGE;
import static java.util.stream.Collectors.joining;

public class RunnerProvider {
    public static final Logger log = LoggerFactory.getLogger(RunnerProvider.class);

    public static final Map<String, AppRunner.Factory> default_providers = new HashMap<String, AppRunner.Factory>() {{
        put("package.json", (cfg, name, folder) -> new NodeRunner(folder, cfg.nodeExecutable(), cfg.npmExecutable()));
        put("pom.xml", (cfg, name, folder) -> new MavenRunner(folder, cfg.javaHomeProvider(), CLEAN_AND_PACKAGE));
        put("project.clj", (cfg, name, folder) -> new LeinRunner(folder, cfg.leinJavaCommandProvider(), cfg.leinCommandProvider()));
    }};

    private final Config config;
    private final Map<String, AppRunner.Factory> providers;

    public RunnerProvider(Config config, Map<String, AppRunner.Factory> providers) {
        this.config = config;
        this.providers = providers;
    }

    public AppRunner runnerFor(String appName, File projectRoot) throws UnsupportedProjectTypeException {
        for (Map.Entry<String, AppRunner.Factory> e : providers.entrySet()) {
            File projectFile = new File(projectRoot, e.getKey());
            if (projectFile.isFile()) {
                AppRunner runner = e.getValue().appRunner(config, appName, projectRoot);
                log.info("Using " + runner.getClass().getSimpleName() + " for " + appName);
                return runner;
            }
        }

        throw new UnsupportedProjectTypeException("No app runner found for " + appName);
    }

    public String describeRunners() {
        return providers.entrySet().stream()
                .map(e -> e.getKey() + " -> " + e.getValue())
                .collect(joining("\n"));
    }
}
