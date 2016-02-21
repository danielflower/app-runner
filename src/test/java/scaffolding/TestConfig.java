package scaffolding;

import com.danielflower.apprunner.Config;

import java.io.IOException;

public class TestConfig {
    public static final Config config;

    static {
        try {
            config = Config.load(new String[]{"sample-config.properties"});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
