package scaffolding;

import com.danielflower.apprunner.Config;
import com.danielflower.apprunner.mgmt.AppManager;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class TestConfig {
    public static final Config config;

    static {
        try {
            config = Config.load(new String[]{"sample-config.properties"});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String,String> testEnvVars(int port, String appName) throws IOException {
        File tempDir = new File("target/testapp/" + appName + "/temp");
        File dataDir = new File("target/testapp/" + appName + "/data/" + UUID.randomUUID());
        FileUtils.forceMkdir(dataDir);
        FileUtils.forceMkdir(tempDir);
        return AppManager.createAppEnvVars(port, appName, dataDir, tempDir);
    }
}
