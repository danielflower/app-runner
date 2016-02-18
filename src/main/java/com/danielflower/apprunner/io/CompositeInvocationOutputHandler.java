package com.danielflower.apprunner.io;

import java.io.IOException;
import java.io.Writer;

public class CompositeInvocationOutputHandler {
    final Writer[] writers;

    public CompositeInvocationOutputHandler(Writer... writers) {
        this.writers = writers;
    }

    public void writeLine(String line) throws IOException {
        for (Writer writer : writers)
            writer.write(line);
    }
}
