package com.danielflower.apprunner.mgmt;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.json.JSONObject;

import java.util.Date;

public class GitCommit {

    public final String id;
    public final Date commitDate;
    public final String author;
    public final String message;

    public GitCommit(String id, Date commitDate, String author, String message) {
        this.id = id;
        this.commitDate = commitDate;
        this.author = author;
        this.message = message;
    }

    public static GitCommit fromHEAD(Git git) throws Exception {
        ObjectId head = git.getRepository().resolve("HEAD");
        if (head != null) {
            RevCommit mostRecentCommit;
            try (RevWalk walk = new RevWalk(git.getRepository())) {
                mostRecentCommit = walk.parseCommit(head);
            }
            Date commitDate = new Date(1000L * mostRecentCommit.getCommitTime());
            String id = mostRecentCommit.getId().name();
            PersonIdent author = mostRecentCommit.getAuthorIdent();
            return new GitCommit(id, commitDate, author.getName(), mostRecentCommit.getFullMessage());
        } else {
            return null;
        }
    }


    public JSONObject toJSON() {
        return new JSONObject()
            .put("id", id)
            .put("date", commitDate.getTime())
            .put("author", author)
            .put("message", message);
    }
}
