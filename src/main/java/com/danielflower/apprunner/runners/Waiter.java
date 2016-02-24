package com.danielflower.apprunner.runners;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class Waiter implements AutoCloseable {
    public static final Logger log = LoggerFactory.getLogger(Waiter.class);

    private static final long POLL_INTERVAL = 500;
    private Predicate<HttpClient> predicate;
    private final String name;
    private final long timeout;
    private final TimeUnit unit;
    private final HttpClient client = new HttpClient();

    public Waiter(String name, Predicate<HttpClient> predicate, long timeout, TimeUnit unit) {
        this.predicate = predicate;
        this.name = name;
        this.timeout = timeout;
        this.unit = unit;
    }

    public void or(Predicate<HttpClient> other) {
        predicate = predicate.or(other);
    }

    public void blockUntilReady() throws Exception {
        client.start();
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < unit.toMillis(timeout)) {
            Thread.sleep(POLL_INTERVAL);
            if (predicate.test(client)) {
                return;
            }
            log.info("Waiting for start up of " + name);
        }

        throw new TimeoutException();
    }

    @Override
    public void close() throws Exception {
        client.stop();
    }

    public static Waiter waitForApp(String name, int port) {
        URI url = URI.create("http://localhost:" + port + "/" + name + "/");
        return waitFor(name, url, 30, TimeUnit.SECONDS);
    }

    public static Waiter waitFor(String name, URI url, long timeout, TimeUnit unit) {
        return new Waiter(name, client -> {
            try {
                client.GET(url);
                return true;
            } catch (InterruptedException e) {
                return true; // erg... really want to bubble this but can't
            } catch (Exception ex) {
                return false;
            }
        }, timeout, unit);
    }
}
