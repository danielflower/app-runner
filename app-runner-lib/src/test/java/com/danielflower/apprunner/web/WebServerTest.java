package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import com.danielflower.apprunner.FileSandbox;
import com.danielflower.apprunner.mgmt.SystemInfo;
import com.danielflower.apprunner.runners.AppRunnerFactoryProvider;
import com.danielflower.apprunner.web.v1.AppResource;
import com.danielflower.apprunner.web.v1.SystemResource;
import io.muserver.Method;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WebServerTest {

    private static final int PROXY_TIMEOUT = 3000;
    private HttpClient client;
    private WebServer webServer;
    private ProxyMap proxyMap = new ProxyMap();
    private TestServer appServer;
    private String webServerUrl;

    @Before
    public void setup() throws Exception {
        client = new HttpClient();
        client.setFollowRedirects(false);
        client.start();
        AppEstate estate = new AppEstate(proxyMap, fileSandbox(),
            new AppRunnerFactoryProvider(new ArrayList<>()));
        int port = WebServer.getAFreePort();
        webServerUrl = "http://localhost:" + port;
        SystemInfo systemInfo = SystemInfo.create();
        webServer = new WebServer(port, -1, null, null, -1, proxyMap, "test-app",
            new SystemResource(systemInfo, new AtomicBoolean(true), new ArrayList<>(), null), new AppResource(estate, systemInfo, fileSandbox()), PROXY_TIMEOUT, PROXY_TIMEOUT, "apprunner", 500 * 1024 * 1024);
        webServer.start();
        appServer = new TestServer();
    }

    @Test
    public void slowStuffTimesOut() throws Exception {
        proxyMap.add("test-app", appServer.url);
        ContentResponse resp = client.GET(webServerUrl + "/test-app/slow?millis=" + (PROXY_TIMEOUT + 1000));
        assertThat(resp.getStatus(), is(504));
    }

    public static FileSandbox fileSandbox() {
        File root = new File("target/test-sandboxes/" + UUID.randomUUID());
        try {
            FileUtils.forceMkdir(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new FileSandbox(root);
    }

    @After
    public void stop() throws Exception {
        client.stop();
        webServer.close();
        appServer.close();
    }

    @Test
    public void aRequestToTheRootResultsInARedirect() throws Exception {
        proxyMap.add("test-app", appServer.url);

        ContentResponse resp = client.GET(webServerUrl + "/");
        assertThat(resp.getStatus(), is(302));
        String location = resp.getHeaders().get("Location");
        assertThat(location, is(webServerUrl + "/test-app"));
        resp = client.GET(location);
        location = resp.getHeaders().get("Location");
        assertThat(resp.getStatus(), is(302));
        assertThat(location, is(webServerUrl + "/test-app/"));

    }

    @Test
    public void prefixesCanBeProxiedToOtherServices() throws Exception {
        proxyMap.add("sample-app", appServer.url);

        ContentResponse resp = client.GET(webServerUrl + "/sample-app/");
        assertThat(resp.getContentAsString(), containsString("Hello from test server"));
        assertThat(resp.getStatus(), is(200));
    }

    @Test
    public void nonProxiedAndOtherNonSpecialURLsResultIn404s() throws Exception {
        ContentResponse resp = client.GET(webServerUrl + "/blahblabhlbah");
        assertThat(resp.getStatus(), is(404));
    }


    private static class TestServer implements AutoCloseable {
        private final MuServer server;
        final URL url;

        TestServer() throws Exception {
            server = MuServerBuilder.httpServer()
                .addHandler(Method.GET, "/test-app", (request, response, pathParams) -> {
                    response.redirect("/test-app/");
                })
                .addHandler(Method.GET, "/test-app/slow", (request, response, pathParams) -> {
                    try {
                        Thread.sleep(Long.parseLong(request.query().get("millis")));
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                    }
                    response.write("This was slow");
                })
                .addHandler((request, response) -> {
                    response.headers().set("Server", "Test-Server");
                    response.write("Hello from test server");
                    return true;
                })
                .start();

            url = server.uri().resolve("/test-app").toURL();
        }

        public void close() {
            server.stop();
        }
    }
}
