package com.danielflower.apprunner.mgmt;

import java.io.IOException;
import java.util.Map;

public interface GitRepoLoader {
    Map<String, String> loadAll() throws Exception;

    void save(String name, String gitUrl) throws IOException;

    void delete(String name) throws IOException;
}
