package com.danielflower.apprunner.web;

import io.muserver.*;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class AppUriMapperTest {

    private ProxyMap proxyMap = new ProxyMap();
    private AppUriMapper mapper = new AppUriMapper(proxyMap);

    @Test
    public void nonMatchesReturnNull() throws Exception {
        assertThat(mapper.mapFrom(mockRequest("/")), is(nullValue()));
        assertThat(mapper.mapFrom(mockRequest("/blah")), is(nullValue()));
        assertThat(mapper.mapFrom(mockRequest("/blah/")), is(nullValue()));
        assertThat(mapper.mapFrom(mockRequest("/blah/car")), is(nullValue()));
        assertThat(mapper.mapFrom(mockRequest("/blah/car?huh=har")), is(nullValue()));
    }

    @Test
    public void matchesReturnUriInMap() throws Exception {
        String appUrl = "http://localhost:49999/app-runner-home";
        proxyMap.add("app-runner-home", new URL(appUrl));
        proxyMap.add("app-creator", new URL("http://localhost:53888/app-creator"));
        assertThat(mapper.mapFrom(mockRequest("/")), is(nullValue()));
        assertThat(mapper.mapFrom(mockRequest("/blah")), is(nullValue()));
        assertThat(mapper.mapFrom(mockRequest("/app-runner-home")).toString(), is(appUrl));
        assertThat(mapper.mapFrom(mockRequest("/app-runner-home?ni%20hao=wo%20hao")).toString(), is(appUrl + "?ni%20hao=wo%20hao"));
        assertThat(mapper.mapFrom(mockRequest("/app-runner-home/")).toString(), is(appUrl + "/"));
        assertThat(mapper.mapFrom(mockRequest("/app-runner-home/?blah=ha")).toString(), is(appUrl + "/?blah=ha"));
        assertThat(mapper.mapFrom(mockRequest("/app-runner-home/home/page?blah=ha")).toString(), is(appUrl + "/home/page?blah=ha"));
    }

    private MuRequest mockRequest(String relativeUri) {
        return new MuRequest() {
            @Override
            public String contentType() {
                return null;
            }

            @Override
            public long startTime() {
                return 0;
            }

            @Override
            public Method method() {
                return null;
            }

            @Override
            public URI uri() {
                return URI.create("https://apprunner.example.org" + relativeUri);
            }

            @Override
            public URI serverURI() {
                return null;
            }

            @Override
            public Headers headers() {
                return null;
            }

            @Override
            public Optional<InputStream> inputStream() {
                return Optional.empty();
            }

            @Override
            public String readBodyAsString() throws IOException {
                return null;
            }

            @Override
            public List<UploadedFile> uploadedFiles(String name) throws IOException {
                return null;
            }

            @Override
            public UploadedFile uploadedFile(String name) throws IOException {
                return null;
            }

            @Override
            public RequestParameters query() {
                return null;
            }

            @Override
            public RequestParameters form() throws IOException {
                return null;
            }

            @Override
            public String parameter(String name) {
                return null;
            }

            @Override
            public List<String> parameters(String name) {
                return null;
            }

            @Override
            public String formValue(String name) throws IOException {
                return null;
            }

            @Override
            public List<String> formValues(String name) throws IOException {
                return null;
            }

            @Override
            public Set<Cookie> cookies() {
                return null;
            }

            @Override
            public Optional<String> cookie(String name) {
                return Optional.empty();
            }

            @Override
            public String contextPath() {
                return null;
            }

            @Override
            public String relativePath() {
                return null;
            }

            @Override
            public Object state() {
                return null;
            }

            @Override
            public void state(Object value) {

            }

            @Override
            public Object attribute(String key) {
                return null;
            }

            @Override
            public void attribute(String key, Object value) {

            }

            @Override
            public AsyncHandle handleAsync() {
                return null;
            }

            @Override
            public String remoteAddress() {
                return null;
            }

            @Override
            public MuServer server() {
                return null;
            }

            @Override
            public boolean isAsync() {
                return false;
            }

            @Override
            public String protocol() {
                return null;
            }
        };
    }
}