package com.danielflower.apprunner.web;

import com.danielflower.apprunner.problems.AppRunnerException;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        handlers.addHandler(createHomeRedirect());
        handlers.addHandler(createRestService());
        handlers.addHandler(createReverseProxy(proxyMap));
        jettyServer.setHandler(handlers);

        jettyServer.start();

        port = ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
        log.info("Started web server at " + baseUrl());
    }

    private Handler createRestService() {
        ServletContextHandler sch = new ServletContextHandler();
        sch.setContextPath("/api");

        AppResource resource = new AppResource(estate);
        ResourceConfig rc = new ResourceConfig();
        rc.register(resource);

        ServletContainer sc = new ServletContainer(rc);
        ServletHolder holder = new ServletHolder(sc);
        sch.addServlet(holder, "/*");
        return sch;
    }

    private Handler createHomeRedirect() {
        return new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if ("/".equals(target)) {
                    response.sendRedirect("/app-runner-home");
                    baseRequest.setHandled(true);
                }
            }
        };
    }

    private static ServletHandler createReverseProxy(ProxyMap proxyMap) {
        ServletHandler proxyHandler = new ServletHandler();
        AsyncProxyServlet servlet = new ReverseProxy(proxyMap);
        ServletHolder proxyServletHolder = new ServletHolder(servlet);
        proxyServletHolder.setAsyncSupported(true);
        proxyServletHolder.setInitParameter("maxThreads", "100");
        proxyHandler.addServletWithMapping(proxyServletHolder, "/*");
        return proxyHandler;
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
