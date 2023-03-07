package com.danielflower.apprunner.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyMap {
    public static final Logger log = LoggerFactory.getLogger(ProxyMap.class);
    private final ConcurrentHashMap<String, URL> mapping = new ConcurrentHashMap<>();

    public void add(String prefix, URL url) {
        URL old = mapping.put(prefix, url);
        if (old == null) {
            log.info(prefix + " maps to " + url);
        } else {
            log.info(prefix + " maps to " + url + " (previously " + old + ")");
        }
    }

    public void remove(String prefix) {
        URL remove = mapping.remove(prefix);
        if (remove == null) {
            log.info("Removed " + prefix + " mapping to " + remove);
        }
    }

    public URL get(String prefix) {
        return mapping.getOrDefault(prefix, null);
    }
}
