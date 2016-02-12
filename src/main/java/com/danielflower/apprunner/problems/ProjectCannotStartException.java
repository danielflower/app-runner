package com.danielflower.apprunner.problems;

public class ProjectCannotStartException extends AppRunnerException {
    public ProjectCannotStartException(String message) {
        super(message);
    }

    public ProjectCannotStartException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectCannotStartException(Throwable cause) {
        super(cause);
    }
}
