package io.muserver.murp;

import io.muserver.MuRequest;

import java.net.URI;

public interface UriMapper {

    /**
     * Gets a URI to proxy to based on the given request.
     * @param request The client request to potentially proxy.
     * @return A URI if this request should be proxied; otherwise null.
     */
    URI mapFrom(MuRequest request) throws Exception;

}
