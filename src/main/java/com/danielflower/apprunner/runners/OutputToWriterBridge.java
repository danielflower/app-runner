package com.danielflower.apprunner.runners;

import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

public class OutputToWriterBridge implements InvocationOutputHandler {
    public static final Logger log = LoggerFactory.getLogger(OutputToWriterBridge.class);
    private final Writer writer;

    public OutputToWriterBridge(Writer writer) {
        this.writer = writer;
    }

    public void consumeLine(String line) {
        try {
            writer.write(line + "\n");
            writer.flush();
        } catch (IOException e) {
            log.info("Error while writing", e);
        }
    }
}
