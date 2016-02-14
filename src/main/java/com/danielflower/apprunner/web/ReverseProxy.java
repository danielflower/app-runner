package com.danielflower.apprunner.web;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ReverseProxy extends AsyncProxyServlet {
    public static final Logger log = LoggerFactory.getLogger(ReverseProxy.class);

    private final ProxyMap proxyMap;

    public ReverseProxy(ProxyMap proxyMap) {
        this.proxyMap = proxyMap;
    }

    @Override
    protected String filterServerResponseHeader(HttpServletRequest clientRequest, Response serverResponse, String headerName, String headerValue) {
        if (headerName.equalsIgnoreCase("location")) {
            URI targetUri = serverResponse.getRequest().getURI();
            String toReplace = targetUri.getScheme() + "://" + targetUri.getAuthority();
            if (headerValue.startsWith(toReplace)) {
                headerValue = clientRequest.getScheme() + "://" + clientRequest.getHeader("host")
                    + headerValue.substring(toReplace.length());
                log.info("Rewrote location header to " + headerValue);
                return headerValue;
            }
        }
        return super.filterServerResponseHeader(clientRequest, serverResponse, headerName, headerValue);
    }

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
        String uri = clientRequest.getRequestURI();
        Pattern pattern = Pattern.compile("/([^/?]+)(.*)");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            URL url = proxyMap.get(prefix);
            if (url != null) {
                String query = isEmpty(clientRequest.getQueryString()) ? "" : "?" + clientRequest.getQueryString();
                String newTarget = url.toString() + matcher.group(2) + query;
                log.info("Proxying to " + newTarget);
                return newTarget;
            }
        }
        log.info("No proxy target configured for " + uri);
        return null;
    }

    @Override
    protected void onProxyRewriteFailed(HttpServletRequest clientRequest, HttpServletResponse proxyResponse) {
        // this is called if rewriteTarget returns null2
        sendProxyResponseError(clientRequest, proxyResponse, 404);
    }
}
