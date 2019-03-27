package io.muserver.murp;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * A builder for a Jetty HTTP Client that is suitable for use in a reverse proxy.
 */
public class HttpClientBuilder {

    private long idleTimeoutMillis = 60000;
    private long connectTimeoutMillis = 15000;
    private long addressResolutionTimeoutMillis = 15000;
    private int maxConnectionsPerDestination = 256;
    private SslContextFactory sslContextFactory;

    /**
     * @param idleTimeoutMillis the max time, in milliseconds, a connection can be idle (that is, without traffic of bytes in either direction)
     * @return This builder
     * @see HttpClient#setIdleTimeout(long)
     */
    public HttpClientBuilder withIdleTimeoutMillis(long idleTimeoutMillis) {
        this.idleTimeoutMillis = idleTimeoutMillis;
        return this;
    }

    /**
     * @param connectTimeoutMillis the max time, in milliseconds, a connection can take to connect to destinations
     * @return This builder
     * @see HttpClient#setConnectTimeout(long)
     */
    public HttpClientBuilder withConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    /**
     * @param addressResolutionTimeoutMillis the socket address resolution timeout
     * @return This builder
     * @see HttpClient#setAddressResolutionTimeout(long)
     */
    public HttpClientBuilder withAddressResolutionTimeoutMillis(long addressResolutionTimeoutMillis) {
        this.addressResolutionTimeoutMillis = addressResolutionTimeoutMillis;
        return this;
    }

    /**
     * @param maxConnectionsPerDestination the max number of connections to open to each destination
     * @return This builder
     * @see HttpClient#setMaxConnectionsPerDestination(int)
     */
    public HttpClientBuilder withMaxConnectionsPerDestination(int maxConnectionsPerDestination) {
        this.maxConnectionsPerDestination = maxConnectionsPerDestination;
        return this;
    }

    /**
     * The SSL Context Factory to use. The default one trusts all servers.
     * @param sslContextFactory The SSL Context factory to use, or null for the default
     * @return This builder
     */
    public HttpClientBuilder withSslContextFactory(SslContextFactory sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
        return this;
    }

    /**
     * @return a new client builder with reasonable defaults
     */
    public static HttpClientBuilder httpClient() {
        return new HttpClientBuilder();
    }

    /**
     * Creates and starts an HTTP Client that is suitable for use in a reverse proxy
     * @return A started Jetty HttpClient
     */
    public HttpClient build() {

        int selectors = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

        SslContextFactory scf = sslContextFactory != null ? sslContextFactory : new SslContextFactory(true);

        HttpClient client = new HttpClient(new HttpClientTransportOverHTTP(selectors), scf);
        client.setFollowRedirects(false);
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setMaxConnectionsPerDestination(maxConnectionsPerDestination);
        client.setAddressResolutionTimeout(addressResolutionTimeoutMillis);
        client.setConnectTimeout(connectTimeoutMillis);
        client.setIdleTimeout(idleTimeoutMillis);
        client.setUserAgentField(null);
        try {
            client.start();
        } catch (Exception e) {
            throw new RuntimeException("Error while starting HTTP Client for reverse proxy", e);
        }

        client.getContentDecoderFactories().clear();

        return client;
    }
}
