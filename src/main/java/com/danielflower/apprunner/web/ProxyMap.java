package com.danielflower.apprunner.web;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyMap {
    private final ConcurrentHashMap<String, URL> mapping = new ConcurrentHashMap<>();

    public void add(String prefix, URL url) {
        mapping.put(prefix, url);
    }

    public void remove(String prefix, URL url) {
        mapping.remove(prefix);
    }

    public URL get(String prefix) {
        return mapping.getOrDefault(prefix, null);
    }
}
