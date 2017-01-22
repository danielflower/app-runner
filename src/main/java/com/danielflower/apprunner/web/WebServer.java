package com.danielflower.apprunner.web;

import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.web.v1.AppResource;
import com.danielflower.apprunner.web.v1.SystemResource;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class WebServer implements AutoCloseable {
    public static final Logger log = LoggerFactory.getLogger(WebServer.class);
    private final ProxyMap proxyMap;
    private Server jettyServer;
    private final String defaultAppName;
    private final SystemResource systemResource;
    private final AppResource appResource;

    public WebServer(Server jettyServer, ProxyMap proxyMap, String defaultAppName, SystemResource systemResource, AppResource appResource) {
        this.jettyServer = jettyServer;
        this.proxyMap = proxyMap;
        this.defaultAppName = defaultAppName;
        this.systemResource = systemResource;
        this.appResource = appResource;
    }

    public static int getAFreePort() {
        try {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                return serverSocket.getLocalPort();
            }
        } catch (IOException e) {
            throw new AppRunnerException("Unable to get a port", e);
        }
    }

    public void start() throws Exception {

        HandlerList handlers = new HandlerList();
        handlers.addHandler(createHomeRedirect());
        handlers.addHandler(createRestService());
        handlers.addHandler(createReverseProxy(proxyMap));
        jettyServer.setHandler(handlers);
        jettyServer.start();
        for (Connector connector : jettyServer.getConnectors()) {
            log.info("Endpoint: " + StringUtils.join(connector.toString().split("[{}]+"), " ", 1, 3));
        }
        log.info("Started web server");
    }

    private Handler createRestService() {
        ResourceConfig rc = new ResourceConfig();
        rc.register(systemResource);
        rc.register(appResource);
        rc.register(JacksonFeature.class);
        rc.register(CORSFilter.class);
        SwaggerDocs.registerSwaggerJsonResource(rc);
        rc.addProperties(new HashMap<String,Object>() {{
            // Turn off buffering so results can be streamed
            put(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 0);
        }});

        ServletHolder holder = new ServletHolder(new ServletContainer(rc));

        ServletContextHandler sch = new ServletContextHandler();
        sch.setContextPath("/api/v1");
        sch.addServlet(holder, "/*");

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setHandler(sch);
        return gzipHandler;
    }

    private static class CORSFilter implements ContainerResponseFilter {
        public void filter(ContainerRequestContext request,
                           ContainerResponseContext response) throws IOException {
            response.getHeaders().add("Access-Control-Allow-Origin", "*");
            response.getHeaders().add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization");
            response.getHeaders().add("Access-Control-Allow-Credentials", "true");
            response.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        }
    }

    private Handler createHomeRedirect() {
        return new AbstractHandler() {
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if ("/".equals(target)) {
                    if (StringUtils.isNotEmpty(defaultAppName)) {
                        response.sendRedirect("/" + defaultAppName);
                    } else {
                        response.sendError(400, "You can set a default app by setting the " + Config.DEFAULT_APP_NAME + " property.");
                    }
                    baseRequest.setHandled(true);
                }
            }
        };
    }

    private ServletHandler createReverseProxy(ProxyMap proxyMap) {
        AsyncProxyServlet servlet = new ReverseProxy(proxyMap);
        ServletHolder proxyServletHolder = new ServletHolder(servlet);
        proxyServletHolder.setAsyncSupported(true);
        proxyServletHolder.setInitParameter("maxThreads", "100");
        ServletHandler proxyHandler = new ServletHandler();
        proxyHandler.addServletWithMapping(proxyServletHolder, "/*");
        return proxyHandler;
    }

    public void close() throws Exception {
        jettyServer.stop();
        jettyServer.join();
        jettyServer.destroy();
    }
}
