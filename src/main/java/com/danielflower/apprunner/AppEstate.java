package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.AppDescription;
import com.danielflower.apprunner.mgmt.AppManager;
import com.danielflower.apprunner.problems.AppNotFoundException;
import com.danielflower.apprunner.runners.RunnerProvider;
import com.danielflower.apprunner.runners.UnsupportedProjectTypeException;
import com.danielflower.apprunner.web.ProxyMap;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.eclipse.jgit.api.errors.GitAPIException;
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
    private final List<AppChangedListener> appAddedListeners = new ArrayList<>();
    private final List<AppChangedListener> appDeletedListeners = new ArrayList<>();
    private final RunnerProvider runnerProvider;

    public AppEstate(ProxyMap proxyMap, FileSandbox fileSandbox, RunnerProvider runnerProvider) {
        this.proxyMap = proxyMap;
        this.fileSandbox = fileSandbox;
        this.runnerProvider = runnerProvider;
    }

    public void add(AppDescription appMan) throws IOException {
        this.managers.add(appMan);
        for (AppChangedListener appAddedListener : appAddedListeners) {
            appAddedListener.onAppChanged(appMan);
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

    public AppDescription addApp(String gitUrl, String appName) throws UnsupportedProjectTypeException, IOException, GitAPIException {
        AppManager appMan = AppManager.create(gitUrl, fileSandbox, appName);
        runnerProvider.runnerFor(appName, fileSandbox.repoDir(appName));
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

    public void addAppAddedListener(AppChangedListener listener) {
        this.appAddedListeners.add(listener);
    }

    public void addAppDeletedListener(AppChangedListener listener) {
        this.appDeletedListeners.add(listener);
    }

    public Optional<AppDescription> app(String name) {
        return all().filter(a -> a.name().equals(name)).findFirst();
    }

    public boolean remove(AppDescription appDescription) throws IOException {
        try {
            appDescription.stopApp();
        } catch (Exception e) {
            log.warn("Error while shutting " + appDescription.name(), e);
        }
        for (AppChangedListener appDeletedListener : appDeletedListeners) {
            appDeletedListener.onAppChanged(appDescription);
        }
        return managers.remove(appDescription);
    }

    public interface AppChangedListener {
        void onAppChanged(AppDescription app) throws IOException;
    }

    public String allAppNames() {
        return all()
            .sorted((o1, o2) -> o1.name().compareTo(o2.name()))
            .map(AppDescription::name)
            .collect(Collectors.joining(", "));
    }

    public Stream<AppDescription> appsByStartupOrder(String firstAppToStart) {
        return all().sorted((o1, o2) -> {
            if (o1.name().equalsIgnoreCase(firstAppToStart)) {
                return -1;
            } else if (o2.name().equalsIgnoreCase(firstAppToStart)) {
                return 1;
            }
            return o1.name().compareToIgnoreCase(o2.name());
        });
    }
}
