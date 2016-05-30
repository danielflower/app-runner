package com.danielflower.apprunner.web;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ReverseProxy extends AsyncProxyServlet {
    public static final Logger log = LoggerFactory.getLogger(ReverseProxy.class);

    private final ProxyMap proxyMap;
    public static final Pattern APP_REQUEST = Pattern.compile("/([^/?]+)(.*)");

    public ReverseProxy(ProxyMap proxyMap) {
        this.proxyMap = proxyMap;
    }

    protected void addViaHeader(Request proxyRequest) {
        super.addViaHeader(proxyRequest);
    }

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

    protected String rewriteTarget(HttpServletRequest clientRequest) {
        String uri = clientRequest.getRequestURI();
        Matcher matcher = APP_REQUEST.matcher(uri);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            URL url = proxyMap.get(prefix);
            if (url != null) {
                String query = isEmpty(clientRequest.getQueryString()) ? "" : "?" + clientRequest.getQueryString();
                String newTarget = url.toString() + matcher.group(2) + query;
//                newTarget = newTarget.replaceAll(":[0-9]+/", ":8081/");
                log.info("Proxying to " + newTarget);
                return newTarget;
            }
        }
        log.info("No proxy target configured for " + uri);
        return null;
    }

    protected void onProxyRewriteFailed(HttpServletRequest clientRequest, HttpServletResponse proxyResponse) {
        // this is called if rewriteTarget returns null2
        try {
            proxyResponse.getWriter().write("404 Not Found");
        } catch (IOException e) {
            log.info("Could not write error", e);
        }
        sendProxyResponseError(clientRequest, proxyResponse, 404);
    }

    protected void addProxyHeaders(HttpServletRequest clientRequest, Request proxyRequest) {
        super.addProxyHeaders(clientRequest, proxyRequest);
        proxyRequest.getHeaders().remove("Host");
        proxyRequest.header("Host", clientRequest.getHeader("Host"));
    }
}
