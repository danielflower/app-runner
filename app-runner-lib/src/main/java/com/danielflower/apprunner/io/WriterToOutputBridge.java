package com.danielflower.apprunner.io;

import java.io.IOException;
import java.io.Writer;

public class WriterToOutputBridge extends Writer {
    private final LineConsumer consoleLogHandler;

    public WriterToOutputBridge(LineConsumer consoleLogHandler) {
        this.consoleLogHandler = consoleLogHandler;
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        String s = new String(cbuf, off, len);
        consoleLogHandler.consumeLine(s);
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
    }
}
