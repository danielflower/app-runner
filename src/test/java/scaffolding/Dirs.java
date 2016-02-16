package scaffolding;

import com.danielflower.apprunner.Config;

import java.io.File;
import java.io.IOException;

public class Dirs {
    public static final File javaHome;

    static {
        try {
            javaHome = Config.load(new String[]{"sample-config.properties"}).getDir("JAVA_HOME");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
