package com.danielflower.apprunner;

public class EntryPoint {

    public static void main(String[] args) {
        try {
            App app = new App(Config.load(args));
            app.start();
            Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
        } catch (Throwable t) {
            App.log.error("Error during startup", t);
            System.exit(1);
        }
    }
}
