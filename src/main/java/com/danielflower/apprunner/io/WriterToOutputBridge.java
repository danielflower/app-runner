package com.danielflower.apprunner.io;

import org.apache.maven.shared.invoker.InvocationOutputHandler;

import java.io.IOException;
import java.io.Writer;

public class WriterToOutputBridge extends Writer {
    private final InvocationOutputHandler consoleLogHandler;

    public WriterToOutputBridge(InvocationOutputHandler consoleLogHandler) {
        this.consoleLogHandler = consoleLogHandler;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String s = new String(cbuf, off, len);
        consoleLogHandler.consumeLine(s);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
