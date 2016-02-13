package com.danielflower.apprunner.mgmt;

import java.io.Writer;

public interface AppDescription {
    String name();

    String gitUrl();

    void stopApp() throws Exception;

    void update(Writer writer) throws Exception;
}
