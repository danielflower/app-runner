package com.danielflower.apprunner.runners;

import com.jezhumble.javasysmon.JavaSysMon;
import org.apache.commons.exec.ExecuteWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Killer extends ExecuteWatchdog {
    private static final Logger log = LoggerFactory.getLogger(Killer.class);
    private final JavaSysMon sys = new JavaSysMon();
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
        super.destroyProcess();
        if (process.isAlive()) {
            log.info("Did not shut down cleanly, so killing process forcibly");
            process.destroyForcibly();
        }
    }
}
