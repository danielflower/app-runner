package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.FileSandbox;
import com.danielflower.apprunner.web.WebServerTest;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import scaffolding.AppRepo;

import java.io.IOException;

public class AppManagerTest {

    @Test(expected = ValidationException.class)
    public void namesWithSpacesAreInvalid() throws IOException, GitAPIException {
        AppManager.create(null, null, "some name");
    }

    @Test(expected = NullPointerException.class)
    public void namesCanBeLettersNumbersUnderscoresAndHypens() throws IOException, GitAPIException {
        AppManager.create(null, null, "Some-na_me3");
    }

    @Test
    public void itCanDeleteAndRecreate() throws Exception {
        AppRepo maven = AppRepo.create("maven");
        FileSandbox sandbox = WebServerTest.fileSandbox();
        AppManager appManager = AppManager.create(maven.gitUrl(), sandbox, "my-maven-app");
        appManager.delete();
        AppManager appManager2 = AppManager.create(maven.gitUrl(), sandbox, "my-maven-app");
        appManager2.delete();
    }

}