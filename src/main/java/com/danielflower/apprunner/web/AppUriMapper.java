package com.danielflower.apprunner.web;

import io.muserver.MuRequest;
import io.muserver.murp.UriMapper;

import java.net.URI;
import java.net.URL;

public class AppUriMapper implements UriMapper {

    private final ProxyMap proxyMap;

    public AppUriMapper(ProxyMap proxyMap) {
        this.proxyMap = proxyMap;
    }

    @Override
    public URI mapFrom(MuRequest request) throws Exception {
        String[] segments = request.uri().getPath().split("/", 3);
        if (segments.length > 1 && !segments[1].isEmpty()) {
            URL url = proxyMap.get(segments[1]);
            if (url == null) {
                return null;
            }
            String qs = request.uri().getRawQuery() == null ? "" : "?" + request.uri().getRawQuery();
            return url.toURI().resolve(request.uri().getRawPath() + qs);
        }
        return null;
    }
}
