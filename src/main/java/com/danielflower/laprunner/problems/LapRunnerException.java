package com.danielflower.laprunner.problems;

public class LapRunnerException extends RuntimeException {
    public LapRunnerException(String message) {
        super(message);
    }

    public LapRunnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public LapRunnerException(Throwable cause) {
        super(cause);
    }
}
