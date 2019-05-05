package com.danielflower.apprunner.web;

import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.web.v1.AppResource;
import com.danielflower.apprunner.web.v1.SystemResource;
import io.muserver.*;
import io.muserver.acme.AcmeCertManager;
import io.muserver.handlers.HttpsRedirectorBuilder;
import io.muserver.openapi.OpenAPIObjectBuilder;
import io.muserver.rest.RestHandlerBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

import static io.muserver.ContextHandlerBuilder.context;
import static io.muserver.murp.HttpClientBuilder.httpClient;
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
    private final SSLContextBuilder sslContext;
    private MuServer muServer;
    private final String defaultAppName;
    private final SystemResource systemResource;
    private final AppResource appResource;
    private final int idleTimeout;
    private final int totalTimeout;
    private final String viaName;
    private HttpClient rpClient;

    public WebServer(int httpPort, int httpsPort, SSLContextBuilder sslContext, AcmeCertManager acmeCertManager, int redirectToHttps, ProxyMap proxyMap, String defaultAppName, SystemResource systemResource, AppResource appResource, int idleTimeout, int totalTimeout, String viaName) {
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

        rpClient = httpClient()
            .withIdleTimeoutMillis(idleTimeout)
            .withMaxRequestHeadersSize(maxRequestHeadersSize)
            .build();

        muServer = MuServerBuilder.muServer()
            .withHttpPort(httpPort)
            .withHttpsPort(httpsPort)
            .withHttpsConfig(acmeCertManager != null ? acmeCertManager.createSSLContext() : sslContext)
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
                        RestHandlerBuilder.restHandler(systemResource, appResource)
                            .withCORS(corsConfig()
                                .withAllowedOriginRegex(".*")
                                .withAllowCredentials(true)
                                .withExposedHeaders("content-type", "accept", "authorization")
                            )
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


    public void close() throws Exception {
        if (acmeCertManager != null) {
            acmeCertManager.stop();
        }
        if (muServer != null) {
            muServer.stop();
            muServer = null;
        }
        if (rpClient != null) {
            rpClient.stop();
            rpClient = null;
        }
    }
}
