package com.danielflower.apprunner;

import com.danielflower.apprunner.problems.AppRunnerException;
import com.danielflower.apprunner.problems.InvalidConfigException;
import com.danielflower.apprunner.runners.CommandLineProvider;
import com.danielflower.apprunner.runners.ExplicitJavaHome;
import com.danielflower.apprunner.runners.HomeProvider;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class Config {
    public static final String SERVER_PORT = "appserver.port";
    public static final String DATA_DIR = "appserver.data.dir";
    public static final String DEFAULT_APP_NAME = "appserver.default.app.name";
    public static final String INITIAL_APP_URL = "appserver.initial.app.url";

    public static final String JAVA_HOME = "java.home";
    public static final String LEIN_JAR = "lein.jar";
    public static final String LEIN_JAVA_CMD = "lein.java.cmd";

    public static Config load(String[] commandLineArgs) throws IOException {
        Map<String, String> systemEnv = System.getenv();
        Map<String, String> env = new HashMap<>(systemEnv);
        for (Map.Entry<String, String> s : systemEnv.entrySet()) {
            env.put(s.getKey().toLowerCase().replace('_', '.'), s.getValue());
        }
        for (String key : System.getProperties().stringPropertyNames()) {
            String value = System.getProperty(key);
            env.put(key, value);
        }
        for (String commandLineArg : commandLineArgs) {
            File file = new File(commandLineArg);
            if (file.isFile()) {
                Properties props = new Properties();
                try (FileInputStream inStream = new FileInputStream(file)) {
                    props.load(inStream);
                }
                for (String key : props.stringPropertyNames()) {
                    env.put(key, props.getProperty(key));
                }
            }
        }
        return new Config(env);
    }

    private final Map<String, String> raw;

    public Config(Map<String, String> raw) {
        this.raw = raw;
    }

    public CommandLineProvider leinJavaCommandProvider() {
        return raw.containsKey(LEIN_JAVA_CMD)
            ? (CommandLineProvider) (Map<String, String> env) -> new CommandLine(getFile(LEIN_JAVA_CMD))
            : javaHomeProvider();
    }

    public CommandLineProvider leinCommandProvider() {
        return raw.containsKey(LEIN_JAR)
            ? (Map<String, String> env) -> leinJavaCommandProvider()
                        .commandLine(env)
                        .addArgument("-cp")
                        .addArgument(dirPath(getFile(LEIN_JAR)))
                        .addArgument("-Djava.io.tmpdir=" + env.get("temp"))
                        .addArgument("clojure.main")
                        .addArgument("-m")
                        .addArgument("leiningen.core.main")

            : CommandLineProvider.lein_on_path;
    }

    public String nodeExecutable() {
        return get("node.exec", windowsinize("node"));
    }

    public String npmExecutable() {
        return get("npm.exec", SystemUtils.IS_OS_WINDOWS ? "npm.cmd" : "npm");
    }

    public HomeProvider javaHomeProvider() {
        return raw.containsKey(JAVA_HOME)
            ? new ExplicitJavaHome(getDir(JAVA_HOME))
            : HomeProvider.default_java_home;
    }

    public static String javaExecutableName() {
        return windowsinize("java");
    }

    private static String windowsinize(String command) {
        return SystemUtils.IS_OS_WINDOWS ? command + ".exe" : command;
    }

    public String get(String name, String defaultVal) {
        return raw.getOrDefault(name, defaultVal);
    }

    public String get(String name) {
        String s = get(name, null);
        if (s == null) {
            throw new InvalidConfigException("Missing config item: " + name);
        }
        return s;
    }

    public int getInt(String name) {
        String s = get(name);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("Could not convert " + name + "=" + s + " to an integer");
        }
    }

    public File getDir(String name) {
        File f = new File(get(name));
        if (!f.isDirectory()) {
            throw new AppRunnerException("Could not find " + name + " directory: " + dirPath(f));
        }
        return f;
    }

    public File getOrCreateDir(String name) {
        File f = new File(get(name));
        try {
            FileUtils.forceMkdir(f);
        } catch (IOException e) {
            throw new AppRunnerException("Could not create " + dirPath(f));
        }
        return f;
    }

    public File getFile(String name) {
        File f = new File(get(name));
        if (!f.isFile()) {
            throw new AppRunnerException("Could not find " + name + " file: " + dirPath(f));
        }
        return f;
    }
}

