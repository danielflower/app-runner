package com.danielflower.laprunner.web;

import com.danielflower.laprunner.web.WebServer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WebServerTest {

    private static HttpClient client;
    private static WebServer webServer;

    @BeforeClass
    public static void setup() throws Exception {
        client = new HttpClient(new SslContextFactory(true));
        client.start();
        webServer = aServer();
        webServer.start();
    }

    @AfterClass
    public static void stop() throws Exception {
        client.stop();
        webServer.close();
    }

    @Test
    public void whenTheServerStartsAWelcomePageIsAvailable() throws Exception {
        ContentResponse resp = client.GET(webServer.baseUrl() + "/");
        assertThat(resp.getStatus(), is(200));
        assertThat(resp.getContentAsString(), containsString("The little app runner"));
    }

    private static WebServer aServer() {
        return new WebServer(0);
    }
}
