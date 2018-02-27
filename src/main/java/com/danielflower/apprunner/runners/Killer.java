package com.danielflower.apprunner.runners;

import org.apache.commons.exec.ExecuteWatchdog;

public class Killer extends ExecuteWatchdog {
    private Process process;

    public Killer(long timeout) {
        super(timeout);
    }

    @Override
    public synchronized void start(Process processToMonitor) {
        this.process = processToMonitor;
        super.start(processToMonitor);
    }

    @Override
    public synchronized void destroyProcess() {
        process.destroyForcibly();
        super.destroyProcess();
    }
}
