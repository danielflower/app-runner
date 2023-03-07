package com.danielflower.apprunner.mgmt;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SystemInfoTest {

    @Test
    public void itCanBeCreatedAndReportStuff() {
        // almost a pointless test, but there are chances that exceptions would be thrown during creation so not completely useless
        SystemInfo info = SystemInfo.create();
        assertThat(info.publicKeys, is(not(nullValue())));
        assertThat(info.hostName, is(not(nullValue())));
        assertThat(info.numCpus, is(greaterThan(0)));
        assertThat(info.uptimeInMillis(), is(greaterThanOrEqualTo(0L)));
        assertThat(info.osName, is(not(nullValue())));
        assertThat(info.pid, is(not(nullValue())));
        assertThat(info.user, is(not(nullValue())));
    }

}