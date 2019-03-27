package io.muserver.murp;

import org.eclipse.jetty.client.HttpClient;

public class ReverseProxyBuilder {

    private String viaName;
    private HttpClient httpClient;
    private UriMapper uriMapper;
    private boolean sendLegacyForwardedHeaders;
    private boolean discardClientForwardedHeaders;


}
