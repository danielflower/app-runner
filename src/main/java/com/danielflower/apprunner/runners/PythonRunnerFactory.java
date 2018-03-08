package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.Config;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

/**
 * Created by 44004279 on 16/02/2018.
 */
public class PythonRunnerFactory implements AppRunnerFactory {
        public static final org.slf4j.Logger log = LoggerFactory.getLogger(PythonRunnerFactory.class);
        private final String versionString;
        private final String virtualenvExecutable;
        private final String pythonExecutable;
        private final int majorVersion;
        private final String scriptName;

        PythonRunnerFactory(String virtualenvExecutable, String pythonExecutable, String versionInfo, int majorVersion) {
            this.virtualenvExecutable = virtualenvExecutable;
            this.pythonExecutable = pythonExecutable;
            this.versionString = versionInfo;
            this.majorVersion = majorVersion;
            this.scriptName = "server" + majorVersion + ".py";
        }

        @Override
        public String id() {
            return "python" + majorVersion;
        }

        @Override
        public String sampleProjectName() {
            return "python" + majorVersion + ".zip";
        }

        @Override
        public String description() {
            return "Python " + majorVersion + " apps with pip dependencies";
        }

        @Override
        public String[] startCommands() {
            String[] commands = { virtualenvExecutable + " --python=" + pythonExecutable + " server",
                                    "server/bin/pip install -r requirements.txt",
                                     "server/bin/python server" + majorVersion + ".py" };
            return commands;
        }

        @Override
        public AppRunner appRunner(File folder) {
            return new PythonRunner(folder, virtualenvExecutable, pythonExecutable, scriptName);
        }

        @Override
        public String versionInfo() {
            return versionString;
        }

        @Override
        public boolean canRun(File appDirectory) {
            log.info((new File(appDirectory, scriptName)).toString());
            return new File(appDirectory, scriptName).isFile();
        }

        public static Optional<PythonRunnerFactory> createIfAvailable(Config config, int majorVersion) {
            String virtualenvExecutable = config.pythonVirtualEnvExecutable(majorVersion);
            String pythonExecutable = config.pythonExecutable(majorVersion);
            String pythonVersionInfo;

            Pair<Boolean, String> pythonProc = ProcessStarter.run(new CommandLine(pythonExecutable).addArgument("--version"));
            if (pythonProc.getLeft()) {
                pythonVersionInfo = pythonProc.getRight();
                if (! pythonVersionInfo.startsWith("Python " + majorVersion + ".")){
                    //log.warn("Python version info from " + pythonExecutable + " does not match requested version " + majorVersion + ": " + pythonVersionInfo);
                    return Optional.empty();
                }
            }
            else {
                //log.warn(pythonExecutable + " couldn't be run, so the PythonRunnerFactory has not been created");
                return Optional.empty();
            }

            Pair<Boolean, String> virtualenvProc = ProcessStarter.run(new CommandLine(virtualenvExecutable).addArgument("--version"));
            if (virtualenvProc.getLeft()) {
                return Optional.of(new PythonRunnerFactory(virtualenvExecutable, pythonExecutable, pythonVersionInfo, majorVersion));
            }
            else {
                //log.warn("virtualenv couldn't be run, so the PythonRunnerFactory has not been created");
                return Optional.empty();
            }
        }
}
