package com.danielflower.apprunner.io;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CompositeWriterTest {

    @Test public void delegatesWritesToUnderlyingWriters() throws IOException {
        StringBuilderWriter writer2 = new StringBuilderWriter();
        StringBuilderWriter writer1 = new StringBuilderWriter();

        CompositeInvocationOutputHandler compositeInvocationOutputHandler = new CompositeInvocationOutputHandler(writer1, writer2);

        compositeInvocationOutputHandler.writeLine("Hello world");

        assertThat(writer1.toString(), is("Hello world"));
        assertThat(writer2.toString(), is("Hello world"));
    }
}
