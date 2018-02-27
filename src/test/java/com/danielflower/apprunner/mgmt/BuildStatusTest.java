package com.danielflower.apprunner.mgmt;

import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.Instant;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BuildStatusTest {

    @Test
    public void notStartedWorks() {
        JSONObject s = BuildStatus.notStarted(aCommit()).toJSON();
        JSONAssert.assertEquals("{" +
            "status: 'not-built', description: 'This hasn\\'t been built', commit: {}" +
            "}", s, JSONCompareMode.LENIENT);
    }

    @Test
    public void fetchingWorks() {
        Instant startTime = Instant.now();
        JSONObject s = BuildStatus.fetching(startTime).toJSON();
        JSONAssert.assertEquals("{" +
            "status: 'fetching', startTime: '" + startTime.toString() + "', description: 'Fetching changes from git'" +
            "}", s, JSONCompareMode.STRICT);
    }
    @Test
    public void inProgressWorks() {
        Instant now = Instant.now();
        JSONObject s = BuildStatus.inProgress(now, aCommit(), "maven").toJSON();
        JSONAssert.assertEquals("{" +
            "runnerId: 'maven', status: 'building', startTime: '" + now.toString() + "', description: 'Building now...', commit: {}" +
            "}", s, JSONCompareMode.LENIENT);
    }

    @Test
    public void successWorks() {
        Instant start = Instant.now();
        Instant end = start.plusMillis(10000);
        JSONObject s = BuildStatus.success(start, end, aCommit(), "maven").toJSON();
        JSONAssert.assertEquals("{" +
            "status: 'success', startTime: '" + start.toString() + "', endTime: '" + end.toString() + "', " +
            "description: 'Completed successfully in 10 seconds', commit: {}" +
            "}", s, JSONCompareMode.LENIENT);
    }

    @Test
    public void failureWorks() {
        Instant start = Instant.now();
        Instant end = start.plusMillis(10000);
        JSONObject s = BuildStatus.failure(start, end, "Oh no", aCommit(), "maven").toJSON();
        JSONAssert.assertEquals("{" +
            "status: 'failed', startTime: '" + start.toString() + "', endTime: '" + end.toString() + "', " +
            "description: 'Oh no', commit: {}" +
            "}", s, JSONCompareMode.LENIENT);
    }

    @Test
    public void theCommitCanBeNull() {
        JSONObject buildStatus = BuildStatus.notStarted(null).toJSON();
        assertThat(buildStatus.has("commit"), is(false));
    }

    private GitCommit aCommit() {
        return new GitCommit("3688d7063d2d647e3989d62d9770d0dfd0ce3c25", new Date(), "Me", "This was a commit");
    }

}