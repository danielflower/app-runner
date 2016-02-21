package scaffolding;

import com.danielflower.apprunner.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Dirs {
    public static final File javaHome;
    public static final File javaExecutable;
    public static final Optional<File> leinJar;
    public static final File leinJavaExecutable;

    static {
        try {
            Config config = Config.load(new String[]{"sample-config.properties"});
            javaHome = config.getDir("JAVA_HOME");
            javaExecutable = FileUtils.getFile(javaHome, "bin", SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java");
            leinJar = config.hasItem("LEIN_JAR")
                ? Optional.of(config.getFile("LEIN_JAR"))
                : Optional.empty();
            leinJavaExecutable = config.hasItem("LEIN_JAVA_CMD")
                ? config.getFile("LEIN_JAVA_CMD")
                : javaHome;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
