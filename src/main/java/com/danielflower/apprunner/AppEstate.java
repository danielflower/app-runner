package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.AppDescription;
import com.danielflower.apprunner.mgmt.AppManager;
import com.danielflower.apprunner.problems.AppNotFoundException;
import com.danielflower.apprunner.runners.RunnerProvider;
import com.danielflower.apprunner.web.ProxyMap;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppEstate {
    public static final Logger log = LoggerFactory.getLogger(AppEstate.class);

    private final List<AppDescription> managers = new ArrayList<>();
    private final ProxyMap proxyMap;
    private final FileSandbox fileSandbox;
    private final List<AppAddedListener> appAddedListeners = new ArrayList<>();
    private final RunnerProvider runnerProvider;

    public AppEstate(ProxyMap proxyMap, FileSandbox fileSandbox, RunnerProvider runnerProvider) {
        this.proxyMap = proxyMap;
        this.fileSandbox = fileSandbox;
        this.runnerProvider = runnerProvider;
    }

    public void add(AppDescription appMan) throws IOException {
        this.managers.add(appMan);
        for (AppAddedListener appAddedListener : appAddedListeners) {
            appAddedListener.onAppAdded(appMan);
        }
    }

    public Stream<AppDescription> all() {
        return managers.stream();
    }

    public void shutdown() {
        for (AppDescription manager : managers) {
            log.info("Stopping " + manager.name());
            try {
                manager.stopApp();
            } catch (Exception e) {
                log.warn("Error while stopping " + manager.name(), e);
            }
        }
    }

    public AppDescription addApp(String gitUrl, String appName) throws Exception {
        AppManager appMan = AppManager.create(gitUrl, fileSandbox, appName);
        appMan.addListener(proxyMap::add);
        this.add(appMan);
        return appMan;
    }

    public void update(String name, InvocationOutputHandler outputHandler) throws Exception {
        for (AppDescription manager : managers) {
            if (manager.name().equalsIgnoreCase(name)) {
                manager.update(runnerProvider, outputHandler);
                return;
            }
        }

        throw new AppNotFoundException("No app found with name '" + name + "'. Valid names: " + allAppNames());
    }

    public void addAppAddedListener(AppAddedListener listener) {
        this.appAddedListeners.add(listener);
    }

    public Optional<AppDescription> app(String name) {
        return all().filter(a -> a.name().equals(name)).findFirst();
    }

    public boolean remove(AppDescription appDescription) {
        return managers.remove(appDescription);
    }

    public interface AppAddedListener {
        void onAppAdded(AppDescription app) throws IOException;
    }

    public String allAppNames() {
        return all()
            .sorted((o1, o2) -> o1.name().compareTo(o2.name()))
            .map(AppDescription::name)
            .collect(Collectors.joining(", "));
    }
}
