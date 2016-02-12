package com.danielflower.apprunner.web;

import com.danielflower.apprunner.problems.AppRunnerException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class WebServer implements AutoCloseable {
    public static final Logger log = LoggerFactory.getLogger(WebServer.class);
    private int port;
    private final ProxyMap proxyMap;
    private Server jettyServer;

    public WebServer(int port, ProxyMap proxyMap) {
        this.port = port;
        this.proxyMap = proxyMap;
    }

    public void start() throws Exception {
        jettyServer = new Server(port);
        HandlerList handlers = new HandlerList();

        ServletHandler proxyHandler = new ServletHandler();
        ServletHolder proxyServletHolder = new ServletHolder(new ReverseProxy(proxyMap));
        proxyServletHolder.setAsyncSupported(true);
        proxyServletHolder.setInitParameter("maxThreads", "100");
        proxyHandler.addServletWithMapping(proxyServletHolder, "/*");

        handlers.addHandler(proxyHandler);
        jettyServer.setHandler(handlers);

        jettyServer.start();

        port = ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
        log.info("Started web server at " + baseUrl());
    }

    @Override
    public void close() throws Exception {
        jettyServer.stop();
        jettyServer.join();
        jettyServer.destroy();
    }

    public URL baseUrl() {
        try {
            return new URL("http", "localhost", port, "");
        } catch (MalformedURLException e) {
            throw new AppRunnerException(e);
        }
    }

}
