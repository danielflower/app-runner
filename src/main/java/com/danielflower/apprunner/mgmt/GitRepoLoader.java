package com.danielflower.apprunner.mgmt;

import java.util.List;

public interface GitRepoLoader {
    List<String> loadAll() throws Exception;
}
