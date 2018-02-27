package com.danielflower.apprunner.mgmt;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

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

}