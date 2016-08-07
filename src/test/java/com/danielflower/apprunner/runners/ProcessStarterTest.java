package com.danielflower.apprunner.runners;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import static com.danielflower.apprunner.Config.javaExecutableName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class ProcessStarterTest {
    @Test
    public void canTellTheVersionOfStuff() throws Exception {
        Pair<Boolean, String> result = ProcessStarter.run(new CommandLine(javaExecutableName()).addArgument("-version"));
        assertThat(result.getLeft(), equalTo(true));
        assertThat(result.getRight(), containsString("java"));
        assertThat(result.getRight(), not(containsString("-version")));
    }

    @Test
    public void handlesUnknownCommandsGracefully() throws Exception {
        Pair<Boolean, String> result = ProcessStarter.run(new CommandLine("non-existent-thing").addArgument("-version"));
        assertThat(result.getLeft(), equalTo(false));
        assertThat(result.getRight(), equalTo("Not available"));
    }
}
