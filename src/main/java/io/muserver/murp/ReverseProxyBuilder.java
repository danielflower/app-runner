package io.muserver.murp;

import io.muserver.MuHandler;
import io.muserver.MuHandlerBuilder;
import org.eclipse.jetty.client.HttpClient;

import java.util.concurrent.TimeUnit;

public class ReverseProxyBuilder implements MuHandlerBuilder<ReverseProxy> {

    private String viaName;
    private HttpClient httpClient;
    private UriMapper uriMapper;
    private boolean sendLegacyForwardedHeaders;
    private boolean discardClientForwardedHeaders;
    private long totalTimeoutInMillis = TimeUnit.MINUTES.toMillis(5);

    /**
     * If set, then the HTTP protocol and the given name will be added to the <code>Via</code> header.
     * @param viaName The name to add to the <code>Via</code> header, or null to not send it.
     * @return This builder
     */
    public ReverseProxyBuilder withViaName(String viaName) {
        this.viaName = viaName;
        return this;
    }

    /**
     * Specifies the Jetty HTTP client to use to make the request to the target server. It's recommended
     * you do not set this in order to use the default client that is optimised for reverse proxy usage.
     * @param httpClient The HTTP client to use, or null to use the default client.
     * @return This builder
     */
    public ReverseProxyBuilder withHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Required value. Sets the mapper to use for creating target URIs.
     * @param uriMapper A mapper that creates a target URI based on a client request.
     * @return This builder
     */
    public ReverseProxyBuilder withUriMapper(UriMapper uriMapper) {
        this.uriMapper = uriMapper;
        return this;
    }

    /**
     * Murp always sends <code>Forwarded</code> headers, however by default does not send the
     * non-standard <code>X-Forwarded-*</code> headers. Set this to <code>true</code> to enable
     * these legacy headers for older clients that rely on them.
     * @param sendLegacyForwardedHeaders <code>true</code> to forward headers such as <code>X-Forwarded-Host</code>; otherwise <code>false</code>
     * @return This builder
     */
    public ReverseProxyBuilder sendLegacyForwardedHeaders(boolean sendLegacyForwardedHeaders) {
        this.sendLegacyForwardedHeaders = sendLegacyForwardedHeaders;
        return this;
    }

    /**
     * If true, then any <code>Forwarded</code> or <code>X-Forwarded-*</code> headers that are sent
     * from the client to this reverse proxy will be dropped (defaults to false). Set this to <code>true</code>
     * if you do not trust the client.
     * @param discardClientForwardedHeaders <code>true</code> to ignore Forwarded headers from the client; otherwise <code>false</code>
     * @return This builder
     */
    public ReverseProxyBuilder discardClientForwardedHeaders(boolean discardClientForwardedHeaders) {
        this.discardClientForwardedHeaders = discardClientForwardedHeaders;
        return this;
    }

    /**
     * Sets the total request timeout in millis for a proxied request. Defaults to 5 minutes.
     * @param totalTimeoutInMillis The allowed time in milliseconds for a request.
     * @return This builder
     */
    public ReverseProxyBuilder withTotalTimeout(long totalTimeoutInMillis) {
        this.totalTimeoutInMillis = totalTimeoutInMillis;
        return this;
    }

    /**
     * Sets the total request timeout in millis for a proxied request. Defaults to 5 minutes.
     * @param totalTimeout The allowed time for a request.
     * @param unit The timeout unit.
     * @return This builder
     */
    public ReverseProxyBuilder withTotalTimeout(long totalTimeout, TimeUnit unit) {
        return withTotalTimeout(unit.toMillis(totalTimeout));
    }

    /**
     * Creates and returns a new instance of a reverse proxy builder.
     * @return
     */
    public static ReverseProxyBuilder reverseProxy() {
        return new ReverseProxyBuilder();
    }

    /**
     * Creates a new ReverseProxy which is a MuHandler. You can pass the resulting handler directly
     * to {@link io.muserver.MuServerBuilder#addHandler(MuHandler)}
     * @return A MuHandler that acts as a reverse proxy
     */
    @Override
    public ReverseProxy build() {
        if (uriMapper == null) {
            throw new IllegalStateException("A URI mapper must be specified");
        }
        HttpClient client = httpClient;
        if (client == null) {
            client = HttpClientBuilder.httpClient().build();
        }
        return new ReverseProxy(client, uriMapper, totalTimeoutInMillis, viaName, discardClientForwardedHeaders, sendLegacyForwardedHeaders);
    }
}
