package e2e;

import com.danielflower.apprunner.io.Zippy;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scaffolding.Photocopier;

import java.io.File;
import java.io.IOException;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZipSamplesTask {
    private static final Logger log = LoggerFactory.getLogger(ZipSamplesTask.class);

    @Test
    public void zipTheSamplesAndPutThemInTheResourcesDir() throws IOException {
        File outputDir = new File(Photocopier.projectRoot(), "app-runner-lib/src/main/resources/sample-apps");
        if (!outputDir.isDirectory()) {
            throw new RuntimeException("Expected sample app dir at " + fullPath(outputDir));
        }
        for (File file : Photocopier.sampleDir().listFiles(File::isDirectory)) {
            File outputFile = new File(outputDir, file.getName() + ".zip");
            assertThat(!outputFile.exists() || outputFile.delete(), is(true));
            Zippy.zipDirectory(file, outputFile);
            log.info("Created " + fullPath(outputFile));
        }
    }

}
