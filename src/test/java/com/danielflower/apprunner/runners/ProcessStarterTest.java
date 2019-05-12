package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.client.api.ContentResponse;
import org.hamcrest.Matcher;
import org.junit.Test;
import scaffolding.TestConfig;

import static com.danielflower.apprunner.Config.javaExecutableName;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static scaffolding.RestClient.httpClient;

public class ProcessStarterTest {
    static void startAndStop(int attempt, String appName, AppRunner runner, int port, StringBuilderWriter buildLog, StringBuilderWriter consoleLog, Matcher<String> getResponseMatcher, Matcher<String> buildLogMatcher) throws Exception {
        try {
            try (Waiter startupWaiter = Waiter.waitForApp(appName, port)) {
                runner.start(
                    new OutputToWriterBridge(buildLog),
                    new OutputToWriterBridge(consoleLog),
                    TestConfig.testEnvVars(port, appName),
                    startupWaiter);
            }
            try {
                ContentResponse resp = httpClient.GET("http://localhost:" + port + "/" + appName + "/");
                assertThat(resp.getStatus(), is(200));
                assertThat(resp.getContentAsString(), getResponseMatcher);
                assertThat(buildLog.toString(), buildLogMatcher);
            } finally {
                runner.shutdown();
            }
        } catch (Exception e) {
            clearlyShowError(attempt, e, buildLog, consoleLog);
        }
    }

    static void clearlyShowError(int attempt, Exception e, StringBuilderWriter buildLog, StringBuilderWriter consoleLog) throws Exception {
        System.out.println("Failure on attempt " + attempt);
        System.out.println("Build log");
        System.out.println(buildLog);
        System.out.println();
        System.out.println("Console log");
        System.out.println(consoleLog);
        throw e;
    }

    @Test
    public void canTellTheVersionOfStuff() throws Exception {
        Pair<Boolean, String> result = ProcessStarter.run(new CommandLine(javaExecutableName()).addArgument("-version"));
        assertThat(result.getLeft(), equalTo(true));
        assertThat(result.getRight(), anyOf(containsString("java"), containsString("jdk")));
        assertThat(result.getRight(), not(containsString("-version")));
    }

    @Test
    public void handlesUnknownCommandsGracefully() throws Exception {
        Pair<Boolean, String> result = ProcessStarter.run(new CommandLine("non-existent-thing").addArgument("-version"));
        assertThat(result.getLeft(), equalTo(false));
        assertThat(result.getRight(), equalTo("Not available"));
    }
}
