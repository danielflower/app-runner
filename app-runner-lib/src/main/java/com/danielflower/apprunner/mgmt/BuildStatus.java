package com.danielflower.apprunner.mgmt;

import org.json.JSONObject;

import java.time.Instant;


public class BuildStatus {
    public final String status;
    public final Instant startTime;
    public final Instant endTime;
    public final String description;
    public final String gitUrl;
    public final GitCommit gitCommit;
    public final String runnerId;

    public BuildStatus(String status, Instant startTime, Instant endTime, String description, String gitUrl, GitCommit gitCommit, String runnerId) {
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.gitUrl = gitUrl;
        this.gitCommit = gitCommit;
        this.runnerId = runnerId;
    }


    public static BuildStatus notStarted(String gitUrl, GitCommit gitCommit) {
        return new BuildStatus("not-built", null, null, "This hasn't been built", gitUrl, gitCommit, null);
    }

    public static BuildStatus fetching(Instant startTime, String gitUrl) {
        return new BuildStatus("fetching", startTime, null, "Fetching changes from git", gitUrl, null, null);
    }

    public static BuildStatus inProgress(Instant startTime, String gitUrl, GitCommit gitCommit, String runnerId) {
        return new BuildStatus("building", startTime, null, "Building now...", gitUrl, gitCommit, runnerId);
    }

    public static BuildStatus success(Instant startTime, Instant endTime, String gitUrl, GitCommit gitCommit, String runnerId) {
        String success = "Completed successfully in " + ((endTime.toEpochMilli() - startTime.toEpochMilli())/1000) + " seconds";
        return new BuildStatus("success", startTime, endTime, success, gitUrl, gitCommit, runnerId);
    }

    public static BuildStatus failure(Instant startTime, Instant endTime, String message, String gitUrl, GitCommit gitCommit, String runnerId) {
        return new BuildStatus("failed", startTime, endTime, message, gitUrl, gitCommit, runnerId);
    }

    public JSONObject toJSON() {
        return new JSONObject()
            .put("runnerId", runnerId)
            .put("status", status)
            .put("startTime", startTime == null ? null : startTime.toString())
            .put("endTime", endTime == null ? null : endTime.toString())
            .put("description", description)
            .put("gitUrl", gitUrl)
            .put("commit", gitCommit == null ? null : gitCommit.toJSON());
    }
}
