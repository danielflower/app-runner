package scaffolding;

import com.danielflower.apprunner.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

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
