package com.danielflower.apprunner.web;

import com.danielflower.apprunner.problems.AppRunnerException;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class WebServer implements AutoCloseable {
    public static final Logger log = LoggerFactory.getLogger(WebServer.class);
    private int port;
    private Server jettyServer;

    public WebServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        jettyServer = new Server(port);

        HandlerList handlers = new HandlerList();
        handlers.addHandler(resourceHandler());
        jettyServer.setHandler(handlers);

        jettyServer.start();

        port = ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
        log.info("Started web server at " + baseUrl());
    }

    private Handler resourceHandler() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newClassPathResource("/web", false, false));
        resourceHandler.setWelcomeFiles(new String[] {"index.html"});
        resourceHandler.setMinMemoryMappedContentLength(-1);
        return resourceHandler;
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
