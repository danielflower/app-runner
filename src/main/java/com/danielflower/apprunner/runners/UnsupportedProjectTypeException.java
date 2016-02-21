package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.AppRunnerException;

public class UnsupportedProjectTypeException extends AppRunnerException {
    public UnsupportedProjectTypeException(String message) {
        super(message);
    }
}
