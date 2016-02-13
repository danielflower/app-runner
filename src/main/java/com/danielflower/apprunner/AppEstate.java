package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.AppManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class AppEstate {
    public static final Logger log = LoggerFactory.getLogger(AppEstate.class);

    private final List<AppManager> managers = new ArrayList<>();

    public void add(AppManager appMan) {
        this.managers.add(appMan);
    }

    public Stream<AppManager> all() {
        return managers.stream();
    }

    public void shutdown() {
        for (AppManager manager : managers) {
            log.info("Stopping " + manager.name);
            try {
                manager.stopApp();
            } catch (Exception e) {
                log.warn("Error while stopping " + manager.name, e);
            }
        }

    }
}
