package com.danielflower.apprunner.mgmt;

import java.io.IOException;
import java.util.List;

public interface GitRepoLoader {
    List<String> loadAll() throws Exception;

    void save(String name, String gitUrl) throws IOException;
}
