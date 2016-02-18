package com.danielflower.apprunner.runners;

import org.apache.maven.shared.invoker.InvocationOutputHandler;

import java.io.IOException;
import java.io.Writer;

public class OutputToWriterBridge implements InvocationOutputHandler {
    private final Writer writer;

    public OutputToWriterBridge(Writer writer) {
        this.writer = writer;
    }

    public void consumeLine(String line) {
        try {
            writer.write(line + "\n");
            writer.flush();
        } catch (IOException e) {
            MavenRunner.log.info("Error while writing", e);
        }
    }
}
