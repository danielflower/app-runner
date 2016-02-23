package com.danielflower.apprunner.runners;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class WaiterTest {

    @Test
    public void waitsUntilPredicateIsTrue() throws Exception {
        AtomicInteger count = new AtomicInteger();
        try (Waiter waiter = new Waiter("", httpClient -> (count.getAndIncrement() == 2), 1, TimeUnit.MINUTES)) {
            waiter.blockUntilReady();
        }
    }

    @Test(expected = TimeoutException.class)
    public void timesOutIfPredicateNeverReturnsTrue() throws Exception {
        try (Waiter waiter = new Waiter("", httpClient -> false, 1, TimeUnit.MILLISECONDS)) {
            waiter.blockUntilReady();
        }
    }
}
