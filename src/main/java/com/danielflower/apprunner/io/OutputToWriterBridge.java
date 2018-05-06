package com.danielflower.apprunner.io;

import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

public class OutputToWriterBridge implements InvocationOutputHandler, LineConsumer {
    public static final Logger log = LoggerFactory.getLogger(OutputToWriterBridge.class);
    private final Writer writer;

    public OutputToWriterBridge(Writer writer) {
        this.writer = writer;
    }

    public void consumeLine(String line) {
        try {
            writer.write(line + LINE_SEPARATOR);
            writer.flush();
        } catch (IOException e) {
            log.info("Error while writing", e);
        }
    }
}
