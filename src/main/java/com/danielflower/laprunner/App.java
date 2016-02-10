package com.danielflower.laprunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    public static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            WebServer webServer = new WebServer(1337);
            webServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown invoked");
                try {
                    webServer.close();
                } catch (Exception e) {
                    log.info("Error while stopping", e);
                }
                log.info("Shutdown complete");
            }));
        } catch (Throwable t) {
            log.error("Error during startup", t);
            System.exit(1);
        }

    }

}
