package com.danielflower.apprunner.web;

import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.mgmt.ValidationException;
import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.web.v1.AppResource;
import com.danielflower.apprunner.web.v1.SystemResource;
import io.muserver.*;
import io.muserver.acme.AcmeCertManager;
import io.muserver.handlers.HttpsRedirectorBuilder;
import io.muserver.murp.ReverseProxyBuilder;
import io.muserver.openapi.OpenAPIObjectBuilder;
import io.muserver.rest.RestHandlerBuilder;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.util.concurrent.TimeUnit;

import static io.muserver.ContextHandlerBuilder.context;
import static io.muserver.murp.ReverseProxyBuilder.reverseProxy;
import static io.muserver.openapi.InfoObjectBuilder.infoObject;
import static io.muserver.rest.CORSConfigBuilder.corsConfig;

public class WebServer implements AutoCloseable {
    public static final Logger log = LoggerFactory.getLogger(WebServer.class);
    private final AcmeCertManager acmeCertManager;
    private final int redirectToHttps;
    private final ProxyMap proxyMap;
    private final int httpPort;
    private final int httpsPort;
    private final HttpsConfigBuilder sslContext;
    private MuServer muServer;
    private final String defaultAppName;
    private final SystemResource systemResource;
    private final AppResource appResource;
    private final int idleTimeout;
    private final int totalTimeout;
    private final String viaName;
    private final long maxRequestSize;

    public WebServer(int httpPort, int httpsPort, HttpsConfigBuilder sslContext, AcmeCertManager acmeCertManager, int redirectToHttps, ProxyMap proxyMap, String defaultAppName, SystemResource systemResource, AppResource appResource, int idleTimeout, int totalTimeout, String viaName, long maxRequestSize) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.sslContext = sslContext;
        this.acmeCertManager = acmeCertManager;
        this.redirectToHttps = redirectToHttps;
        this.proxyMap = proxyMap;
        this.defaultAppName = defaultAppName;
        this.systemResource = systemResource;
        this.appResource = appResource;
        this.idleTimeout = idleTimeout;
        this.totalTimeout = totalTimeout;
        this.viaName = viaName;
        this.maxRequestSize = maxRequestSize;
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

        int maxRequestHeadersSize = 24 * 1024;

        HttpClient rpClient = ReverseProxyBuilder.createHttpClientBuilder(true).build();

        muServer = MuServerBuilder.muServer()
            .withHttpPort(httpPort)
            .withHttpsPort(httpsPort)
            .withMaxRequestSize(maxRequestSize)
            .withIdleTimeout(Math.max(idleTimeout + 5000, 10 /* minutes */ * 60 * 1000), TimeUnit.MILLISECONDS) // timeout is at least a little longer than configured timeout, or 5 minutes to account for slow API responses
            .withHttpsConfig(acmeCertManager != null ? acmeCertManager.createHttpsConfig() : sslContext)
            .withMaxHeadersSize(maxRequestHeadersSize)
            .addHandler(acmeCertManager == null ? null : acmeCertManager.createHandler())
            .addHandler(redirectToHttps < 1 ? null : HttpsRedirectorBuilder.toHttpsPort(redirectToHttps))
            .addHandler(Method.GET, "/", (request, response, pathParams) -> {
                if (StringUtils.isNotEmpty(defaultAppName)) {
                    response.redirect("/" + defaultAppName);
                } else {
                    response.status(400);
                    response.contentType(ContentTypes.TEXT_PLAIN);
                    response.write("You can set a default app by setting the " + Config.DEFAULT_APP_NAME + " property.");
                }
            })
            .addHandler(context("api")
                .addHandler(context("v1")
                    .addHandler(
                        RestHandlerBuilder.restHandler(appResource, systemResource)
                            .withCORS(corsConfig()
                                .withAllowedOriginRegex(".*")
                                .withAllowedHeaders("content-type")
                                .withAllowCredentials(true)
                                .withExposedHeaders("content-type", "accept", "authorization")
                            )
                            .addExceptionMapper(ValidationException.class, exception -> Response.status(400)
                                .type("application/json")
                                .entity(new JSONObject()
                                .put("message", exception.getMessage())
                                .toString()).build())
                            .withOpenApiJsonUrl("swagger.json")
                            .withOpenApiHtmlUrl("api.html")
                            .withOpenApiDocument(OpenAPIObjectBuilder.openAPIObject()
                                .withInfo(infoObject()
                                    .withTitle("App Runner")
                                    .withDescription("The REST API for App Runner which is used for registering apps, deploying apps, viewing logs etc.")
                                    .withVersion("1.0")
                                    .build())
                            )
                    )
                ))
            .addHandler(reverseProxy()
                .withUriMapper(new AppUriMapper(proxyMap))
                .withTotalTimeout(totalTimeout)
                .withViaName(viaName)
                .sendLegacyForwardedHeaders(true)
                .discardClientForwardedHeaders(false)
                .withHttpClient(rpClient)
            )
            .start();

        if (acmeCertManager != null) {
            acmeCertManager.start(muServer);
        }

        log.info("Started web server at " + muServer.httpsUri() + " / " + muServer.httpUri());
    }


    public void close() {
        if (acmeCertManager != null) {
            acmeCertManager.stop();
        }
        if (muServer != null) {
            muServer.stop();
            muServer = null;
        }
    }
}
