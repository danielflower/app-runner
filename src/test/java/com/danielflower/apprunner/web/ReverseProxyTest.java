package com.danielflower.apprunner.web;

import org.junit.Test;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ReverseProxyTest {

    ProxyMap proxyMap = new ProxyMap();
    ReverseProxy reverseProxy = new ReverseProxy(proxyMap);

    @Test
    public void returnsNullIfNoValueInProxyMap() throws Exception {
        assertThat(reverseProxy.rewriteTarget(request("/blah")), is(nullValue()));
    }

    @Test
    public void returnsAProxiedAddressIfInMap() throws Exception {
        proxyMap.add("my-app", new URL("http://localhost:12345/my-app"));
        assertThat(reverseProxy.rewriteTarget(request("/my-app")), is("http://localhost:12345/my-app"));
        assertThat(reverseProxy.rewriteTarget(request("/my-app/")), is("http://localhost:12345/my-app/"));
        assertThat(reverseProxy.rewriteTarget(request("/my-app/some/thing")), is("http://localhost:12345/my-app/some/thing"));
    }

    @Test
    public void queryStringsSurviveProxying() throws Exception {
        proxyMap.add("my-app", new URL("http://localhost:12345/my-app"));
        assertThat(reverseProxy.rewriteTarget(request("/my-app?blah=ha")), is("http://localhost:12345/my-app?blah=ha"));
        assertThat(reverseProxy.rewriteTarget(request("/my-app/?blah=ha")), is("http://localhost:12345/my-app/?blah=ha"));
        assertThat(reverseProxy.rewriteTarget(request("/my-app/some/thing?blah=ha")), is("http://localhost:12345/my-app/some/thing?blah=ha"));
    }

    private HttpServletRequest request(String path) throws MalformedURLException {
        URL url = new URL("http://localhost" + path);
        return new HttpServletRequest() {
            public String getAuthType() {
                return null;
            }

            public Cookie[] getCookies() {
                return new Cookie[0];
            }

            public long getDateHeader(String name) {
                return 0;
            }

            public String getHeader(String name) {
                return null;
            }

            public Enumeration<String> getHeaders(String name) {
                return null;
            }

            public Enumeration<String> getHeaderNames() {
                return null;
            }

            public int getIntHeader(String name) {
                return 0;
            }

            public String getMethod() {
                return null;
            }

            public String getPathInfo() {
                return null;
            }

            public String getPathTranslated() {
                return null;
            }

            public String getContextPath() {
                return null;
            }

            public String getQueryString() {
                return url.getQuery();
            }

            public String getRemoteUser() {
                return null;
            }

            public boolean isUserInRole(String role) {
                return false;
            }

            public Principal getUserPrincipal() {
                return null;
            }

            public String getRequestedSessionId() {
                return null;
            }

            public String getRequestURI() {
                return url.getPath();
            }

            public StringBuffer getRequestURL() {
                return null;
            }

            public String getServletPath() {
                return null;
            }

            public HttpSession getSession(boolean create) {
                return null;
            }

            public HttpSession getSession() {
                return null;
            }

            public String changeSessionId() {
                return null;
            }

            public boolean isRequestedSessionIdValid() {
                return false;
            }

            public boolean isRequestedSessionIdFromCookie() {
                return false;
            }

            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            public boolean isRequestedSessionIdFromUrl() {
                return false;
            }

            public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
                return false;
            }

            public void login(String username, String password) throws ServletException {
            }

            public void logout() throws ServletException {
            }

            public Collection<Part> getParts() throws IOException, ServletException {
                return null;
            }

            public Part getPart(String name) throws IOException, ServletException {
                return null;
            }

            public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
                return null;
            }

            public Object getAttribute(String name) {
                return null;
            }

            public Enumeration<String> getAttributeNames() {
                return null;
            }

            public String getCharacterEncoding() {
                return null;
            }

            public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
            }

            public int getContentLength() {
                return 0;
            }

            public long getContentLengthLong() {
                return 0;
            }

            public String getContentType() {
                return null;
            }

            public ServletInputStream getInputStream() throws IOException {
                return null;
            }

            public String getParameter(String name) {
                return null;
            }

            public Enumeration<String> getParameterNames() {
                return null;
            }

            public String[] getParameterValues(String name) {
                return new String[0];
            }

            public Map<String, String[]> getParameterMap() {
                return null;
            }

            public String getProtocol() {
                return null;
            }

            public String getScheme() {
                return null;
            }

            public String getServerName() {
                return null;
            }

            public int getServerPort() {
                return 0;
            }

            public BufferedReader getReader() throws IOException {
                return null;
            }

            public String getRemoteAddr() {
                return null;
            }

            public String getRemoteHost() {
                return null;
            }

            public void setAttribute(String name, Object o) {
            }

            public void removeAttribute(String name) {
            }

            public Locale getLocale() {
                return null;
            }

            public Enumeration<Locale> getLocales() {
                return null;
            }

            public boolean isSecure() {
                return false;
            }

            public RequestDispatcher getRequestDispatcher(String path) {
                return null;
            }

            public String getRealPath(String path) {
                return null;
            }

            public int getRemotePort() {
                return 0;
            }

            public String getLocalName() {
                return null;
            }

            public String getLocalAddr() {
                return null;
            }

            public int getLocalPort() {
                return 0;
            }

            public ServletContext getServletContext() {
                return null;
            }

            public AsyncContext startAsync() throws IllegalStateException {
                return null;
            }

            public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
                return null;
            }

            public boolean isAsyncStarted() {
                return false;
            }

            public boolean isAsyncSupported() {
                return false;
            }

            public AsyncContext getAsyncContext() {
                return null;
            }

            public DispatcherType getDispatcherType() {
                return null;
            }
        };
    }
}
