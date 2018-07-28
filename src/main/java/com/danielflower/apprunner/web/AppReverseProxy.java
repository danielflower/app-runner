package com.danielflower.apprunner.web;

import io.muserver.*;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Arrays.asList;

public class AppReverseProxy implements RouteHandler {
    private static final Logger log = LoggerFactory.getLogger(AppReverseProxy.class);
    private static final Set<String> HOP_BY_HOP_HEADERS = Collections.unmodifiableSet(new HashSet<>(asList(
        "keep-alive", "transfer-encoding", "te", "connection", "trailer", "upgrade", "proxy-authorization", "proxy-authenticate")));

    private final AtomicLong counter = new AtomicLong();
    private final HttpClient httpClient;
    private final ProxyMap proxyMap;
    private final long totalTimeoutInMillis;

    private static final String ipAddress;
    static {
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            ip = "127.0.0.1";
            log.info("Could not fine local address so using " + ip);
        }
        ipAddress = ip;
    }

    AppReverseProxy(HttpClient httpClient, ProxyMap proxyMap, long totalTimeoutInMillis) {
        this.httpClient = httpClient;
        this.proxyMap = proxyMap;
        this.totalTimeoutInMillis = totalTimeoutInMillis;
    }

    @Override
    public void handle(MuRequest clientReq, MuResponse clientResp, Map<String, String> pathParams) throws Exception {
        String name = pathParams.get("appName");
        URL targetURL = proxyMap.get(name);
        if (targetURL == null) {
            clientResp.status(404);
            clientResp.contentType(ContentTypes.TEXT_HTML);
            clientResp.write("<h1>404 Not Found</h1>");
            return;
        }
        URI target = targetURL.toURI();
        final long start = System.currentTimeMillis();

        clientResp.headers().remove(HeaderNames.DATE); // so that the target's date can be used

        URI newTarget = new URI(target.getScheme(), target.getUserInfo(), target.getHost(), target.getPort(), clientReq.uri().getPath(), clientReq.uri().getQuery(), clientReq.uri().getFragment());
        final AsyncHandle asyncHandle = clientReq.handleAsync();
        final long id = counter.incrementAndGet();
        log.info("[" + id + "] Proxying from " + clientReq.uri() + " to " + newTarget);

        Request targetReq = httpClient.newRequest(newTarget);
        targetReq.method(clientReq.method().name());
        boolean hasRequestBody = setHeaders(clientReq, targetReq);

        if (hasRequestBody) {
            DeferredContentProvider targetReqBody = new DeferredContentProvider();
            asyncHandle.setReadListener(new RequestBodyListener() {
                @Override
                public void onDataReceived(ByteBuffer buffer) {
                    targetReqBody.offer(buffer);
                }

                @Override
                public void onComplete() {
                    targetReqBody.close();
                }

                @Override
                public void onError(Throwable t) {
                    targetReqBody.failed(t);
                }
            });
            targetReq.content(targetReqBody);
        }

        targetReq.onResponseHeaders(response -> {
            clientResp.status(response.getStatus());
            HttpFields targetRespHeaders = response.getHeaders();
            List<String> customHopByHopHeaders = getCustomHopByHopHeaders(targetRespHeaders.get(HttpHeader.CONNECTION));
            String via = null;
            for (HttpField targetRespHeader : targetRespHeaders) {
                String lowerName = targetRespHeader.getName().toLowerCase();
                if (HOP_BY_HOP_HEADERS.contains(lowerName) || customHopByHopHeaders.contains(lowerName)) {
                    continue;
                }
                String value = targetRespHeader.getValue();
                if (lowerName.equals("via")) {
                    via = value;
                }
                clientResp.headers().add(targetRespHeader.getName(), value);
            }
            clientResp.headers().set(HeaderNames.VIA, appRunnerVia(via));
        });
        targetReq.onResponseContentAsync((response, content, callback) -> asyncHandle.write(content,
            new WriteCallback() {
                @Override
                public void onFailure(Throwable reason) {
                    callback.failed(reason);
                }

                @Override
                public void onSuccess() {
                    callback.succeeded();
                }
            }));
        targetReq.timeout(totalTimeoutInMillis, TimeUnit.MILLISECONDS);
        targetReq.send(result -> {
            try {
                long duration = System.currentTimeMillis() - start;
                if (result.isFailed()) {
                    String errorID = UUID.randomUUID().toString();
                    log.error("Failed to proxy response. ErrorID=" + errorID + " for " + result, result.getFailure());
                    if (!clientResp.hasStartedSendingData()) {
                        clientResp.contentType(ContentTypes.TEXT_HTML);
                        if (result.getFailure() instanceof TimeoutException) {
                            clientResp.status(504);
                            clientResp.write("<h1>504 Gateway Timeout</h1><p>ErrorID=" + errorID + "</p>");
                        } else {
                            clientResp.status(502);
                            clientResp.write("<h1>502 Bad Gateway</h1><p>ErrorID=" + errorID + "</p>");
                        }
                    }
                } else {
                    log.info("[" + id + "] completed in " + duration + "ms: " + result);
                }
            } finally {
                asyncHandle.complete();
            }
        });


    }

    private static boolean setHeaders(MuRequest clientReq, Request targetReq) {
        Headers reqHeaders = clientReq.headers();
        List<String> customHopByHop = getCustomHopByHopHeaders(reqHeaders.get(HeaderNames.CONNECTION));

        boolean hasContentLengthOrTransferEncoding = false;
        for (Map.Entry<String, String> clientHeader : reqHeaders) {
            String key = clientHeader.getKey();
            String lowKey = key.toLowerCase();
            if (HOP_BY_HOP_HEADERS.contains(lowKey) || customHopByHop.contains(lowKey)) {
                continue;
            }
            hasContentLengthOrTransferEncoding |= lowKey.equals("content-length") || lowKey.equals("transfer-encoding");
            targetReq.header(key, clientHeader.getValue());
        }
        String proto = clientReq.uri().getScheme();
        String originHost = clientReq.uri().getAuthority();

        String curVia = clientReq.headers().get(HeaderNames.VIA, null);
        targetReq.header("Via", appRunnerVia(curVia));
        targetReq.header("X-Forwarded-Proto", null);
        targetReq.header("X-Forwarded-Proto", proto);
        targetReq.header("X-Forwarded-Host", null);
        targetReq.header("X-Forwarded-Host", originHost);
        targetReq.header("X-Forwarded-Server", null);

        String murpForwarded = "by=" + ipAddress + "; for=" + clientReq.remoteAddress() + "; host=" + originHost + "; proto=" + proto;
        String curFowarded = reqHeaders.get("Forwarded", null);
        if (curFowarded != null) {
            murpForwarded = curFowarded + ", " + murpForwarded;
        }
        targetReq.header("Forwarded", null);
        targetReq.header("Forwarded", murpForwarded);

        return hasContentLengthOrTransferEncoding;
    }

    private static String appRunnerVia(String curVia) {
        return (curVia == null ? "" : curVia + ", ") + "HTTP/1.1 apprunner";
    }

    private static List<String> getCustomHopByHopHeaders(String connectionHeaderValue) {
        if (connectionHeaderValue == null) {
            return Collections.emptyList();
        }
        List<String> customHopByHop = new ArrayList<>();
        String[] split = connectionHeaderValue.split(" *, *");
        for (String s : split) {
            customHopByHop.add(s.toLowerCase());
        }
        return customHopByHop;
    }
}
