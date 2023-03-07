package com.danielflower.apprunner.problems;

public class AppRunnerException extends RuntimeException {
    public AppRunnerException(String message) {
        super(message);
    }

    public AppRunnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppRunnerException(Throwable cause) {
        super(cause);
    }
}
