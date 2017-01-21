package com.danielflower.apprunner.mgmt;

import org.json.JSONObject;

import java.util.Date;

public class BuildStatus {
    public final String status;
    public final Date startTime;
    public final Date endTime;
    public final String description;
    public final GitCommit gitCommit;
    public final String runnerId;

    public BuildStatus(String status, Date startTime, Date endTime, String description, GitCommit gitCommit, String runnerId) {
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.gitCommit = gitCommit;
        this.runnerId = runnerId;
    }


    public static BuildStatus notStarted(GitCommit gitCommit) {
        return new BuildStatus("not-built", null, null, "This hasn't been built", gitCommit, null);
    }

    public static BuildStatus fetching(Date startTime) {
        return new BuildStatus("fetching", startTime, null, "Fetching changes from git", null, null);
    }

    public static BuildStatus inProgress(Date startTime, GitCommit gitCommit, String runnerId) {
        return new BuildStatus("building", startTime, null, "Building now...", gitCommit, runnerId);
    }

    public static BuildStatus success(Date startTime, Date endTime, GitCommit gitCommit, String runnerId) {
        String success = "Completed successfully in " + ((endTime.getTime() - startTime.getTime())/1000) + " seconds";
        return new BuildStatus("success", startTime, endTime, success, gitCommit, runnerId);
    }

    public static BuildStatus failure(Date startTime, Date endTime, String message, GitCommit gitCommit, String runnerId) {
        return new BuildStatus("failed", startTime, endTime, message, gitCommit, runnerId);
    }

    public JSONObject toJSON() {
        return new JSONObject()
            .put("runnerId", runnerId)
            .put("status", status)
            .put("startTime", startTime == null ? null : startTime.getTime())
            .put("endTime", endTime == null ? null : endTime.getTime())
            .put("description", description)
            .put("commit", gitCommit == null ? null : gitCommit.toJSON());
    }
}
