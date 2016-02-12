package com.danielflower.laprunner.web;

import com.danielflower.laprunner.problems.LapRunnerException;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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
        jettyServer.setConnectors(new Connector[] {createSslConnector(port, jettyServer)});

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
            return new URL("https", "localhost", port, "");
        } catch (MalformedURLException e) {
            throw new LapRunnerException(e);
        }
    }

    private static ServerConnector createSslConnector(int port, Server jettyServer) {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(System.getProperty("jetty.keystore.path","src/main/resources/local.keystore"));
        sslContextFactory.setKeyStorePassword(System.getProperty("jetty.keystore.password","password"));
        sslContextFactory.setKeyManagerPassword(System.getProperty("jetty.keymanager.password","password"));

        // Setup HTTP Configuration
        HttpConfiguration httpConf = new HttpConfiguration();
        httpConf.setSecurePort(port);
        httpConf.setSecureScheme("https");
        ServerConnector serverConnector = new ServerConnector(jettyServer,
            new SslConnectionFactory(sslContextFactory,"http/1.1"),
            new HttpConnectionFactory(httpConf));
        serverConnector.setPort(port);
        return serverConnector;
    }

}
