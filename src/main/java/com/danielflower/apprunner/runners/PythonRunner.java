package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.problems.ProjectCannotStartException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PythonRunner implements AppRunner {
    public static final Logger log = LoggerFactory.getLogger(PythonRunner.class);
    public static final String[] startCommands = new String[]{"virtualenv --python=<pythonexe> server", "server/bin/pip install -r requirements.txt", "server/bin/python server<2|3>.py"};

    private final File projectRoot;
    private final String pythonExec;
    private final String virtualenvExec;
    private final String scriptName;
    private ExecuteWatchdog watchDog;

    public PythonRunner(File projectRoot, String virtualenvExec, String pythonExec, String scriptName) {
        this.projectRoot = projectRoot;
        this.virtualenvExec = virtualenvExec;
        this.pythonExec = pythonExec;
        this.scriptName = scriptName;
    }

    @Override
    public File getInstanceDir() {
        return projectRoot;
    }

    public void start(InvocationOutputHandler buildLogHandler, InvocationOutputHandler consoleLogHandler, Map<String, String> envVarsForApp, Waiter startupWaiter) throws ProjectCannotStartException {

        final String virtPythonExec;
        final String virtPipExec;

        if (SystemUtils.IS_OS_WINDOWS) {
            virtPythonExec = projectRoot.getAbsolutePath() + "\\server\\Scripts\\pythonw.exe";
            virtPipExec = projectRoot.getAbsolutePath() + "\\server\\Scripts\\pip.exe";
        }
        else{
            virtPythonExec = projectRoot.getAbsolutePath() + "/server/bin/python";
            virtPipExec = projectRoot.getAbsolutePath() + "/server/bin/pip";
        }

        CommandLine virtualenvCmd = new CommandLine(virtualenvExec)
            .addArgument("--python=" + pythonExec)
            .addArgument("server");

        buildLogHandler.consumeLine("Creating virtualenv: " + StringUtils.join(virtualenvCmd.toStrings(), " "));
        ProcessStarter.run(buildLogHandler, envVarsForApp, virtualenvCmd, projectRoot, TimeUnit.MINUTES.toMillis(30));

        File requirements = new File(projectRoot.getAbsolutePath() + File.separator + "requirements.txt");
        if (requirements.exists()) {
            CommandLine pipCmd = new CommandLine(virtPipExec)
                .addArgument("install")
                .addArgument("-r")
                .addArgument(requirements.getAbsolutePath());

            buildLogHandler.consumeLine("Installing dependencies: " + StringUtils.join(pipCmd.toStrings(), " "));
            ProcessStarter.run(buildLogHandler, envVarsForApp, pipCmd, projectRoot, TimeUnit.MINUTES.toMillis(30));
        }

        CommandLine command = new CommandLine(virtPythonExec)
            .addArgument(scriptName)
            .addArgument("--app-name=" + envVarsForApp.get("APP_NAME")); //Add app name to command line so ps can identify which app is running

        buildLogHandler.consumeLine("Starting python app: " + StringUtils.join(command.toStrings(), " "));

        watchDog = ProcessStarter.startDaemon(buildLogHandler, consoleLogHandler, envVarsForApp, command, projectRoot, startupWaiter);

        buildLogHandler.consumeLine("Python app started.");
    }

    public void shutdown() {
        if (watchDog != null) {
            watchDog.destroyProcess();
            watchDog.stop();
        }
    }
}
