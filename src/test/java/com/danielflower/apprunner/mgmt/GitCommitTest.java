package com.danielflower.apprunner.mgmt;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.PersonIdent;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.Photocopier;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class GitCommitTest {

    @Test
    public void returnsNullForEmptyRepo() throws Exception {
        Git git = emptyRepo();

        assertThat(GitCommit.fromHEAD(git), is(nullValue()));
    }

    @Test
    public void returnsCurrentCommitForNonEmptyRepos() throws Exception {
        Git git = emptyRepo();
        FileRepository repository = (FileRepository) git.getRepository();
        File dir = repository.getDirectory();
        FileUtils.writeStringToFile(new File(dir, "file1"), "Hello", "UTF-8");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit")
            .setAuthor(new PersonIdent("Author Test", "author@email.com"))
            .call();

        FileUtils.writeStringToFile(new File(dir, "file2"), "Hello too", "UTF-8");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Second commit")
            .setAuthor(new PersonIdent("Second contributor", "second@email.com"))
            .call();

        JSONObject actual = GitCommit.fromHEAD(git).toJSON();
        JSONAssert.assertEquals("{" +
            "author: 'Second contributor', message: 'Second commit'" +
            "}", actual, JSONCompareMode.LENIENT);

        assertThat(actual.getLong("date"), Matchers.greaterThanOrEqualTo(System.currentTimeMillis() - 1000));
        assertThat(actual.getString("id"), actual.getString("id").length(), is("3688d7063d2d647e3989d62d9770d0dfd0ce3c25".length()));

    }

    private static Git emptyRepo() throws GitAPIException {
        File dir = Photocopier.folderForSampleProject("blah");
        InitCommand initCommand = Git.init();
        initCommand.setDirectory(dir);
        return initCommand.call();
    }

}