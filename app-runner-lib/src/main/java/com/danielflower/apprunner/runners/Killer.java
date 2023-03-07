package com.danielflower.apprunner.runners;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Killer extends ExecuteWatchdog {
    private static final Logger log = LoggerFactory.getLogger(Killer.class);
    private final CommandLine command;
    private Process process;

    public Killer(long timeout, CommandLine command) {
        super(timeout);
        this.command = command;
    }

    @Override
    public synchronized void start(Process processToMonitor) {
        this.process = processToMonitor;
        super.start(processToMonitor);
    }

    @Override
    public synchronized void destroyProcess() {
        long start = System.currentTimeMillis();
        log.info("Killing " + command);
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    log.info("Killing forcibly " + command);
                    process.destroyForcibly();
                }
                super.destroyProcess();
                boolean stopped = process.waitFor(5, TimeUnit.SECONDS);
                if (stopped) {
                    log.info("Killed in " + ((System.currentTimeMillis() - start) + "ms"));
                } else {
                    log.warn("Could not kill " + command);
                }
            } catch (InterruptedException e) {
                log.info("Interrupted while waiting to kill " + command);
            }
        }
    }
}
