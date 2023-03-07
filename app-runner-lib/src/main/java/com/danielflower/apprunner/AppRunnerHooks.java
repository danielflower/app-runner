package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.ValidationException;

public interface AppRunnerHooks {

    default void validateGitUrl(String gitUrl) throws ValidationException {}

}
