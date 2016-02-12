package com.danielflower.laprunner.problems;

public class ProjectCannotStartException extends LapRunnerException {
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
