package com.danielflower.apprunner;

/**
 * An entry point for running the server locally that uses sample-config.properties for config.
 */
public class RunLocal {

    public static void main(String[] args) {
        System.setProperty("logback.configurationFile", "src/test/resources/logback-test.xml");
        App.main(new String[] { "sample-config.properties" });
    }

}
