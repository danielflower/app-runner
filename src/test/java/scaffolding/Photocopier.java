package scaffolding;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static org.apache.commons.io.FilenameUtils.separatorsToSystem;

public class Photocopier {

    public static File sampleDir() {
        return new File(separatorsToSystem("sample-apps"));
    }

    public static File copySampleAppToTempDir(String sampleAppName) throws IOException {
        String pathname = FilenameUtils.concat(dirPath(sampleDir()), sampleAppName);
        File source = new File(pathname);
        if (!source.isDirectory()) {
            source = new File(separatorsToSystem("../") + pathname);
        }
        if (!source.isDirectory()) {
            throw new RuntimeException("Could not find module " + sampleAppName + " at " + new File(pathname) + " nor " + dirPath(source));
        }

        File target = folderForSampleProject(sampleAppName);
        FileUtils.copyDirectory(source, target);
        return target;
    }

    public static File folderForSampleProject(String moduleName) {
        return new File(separatorsToSystem("target/samples/" + UUID.randomUUID() + "/" + moduleName));
    }

    public static File tempFile(String name) throws IOException {
        File file = new File(separatorsToSystem("target/temp/" + UUID.randomUUID() + "/" + name));
        FileUtils.touch(file);
        return file;
    }
}
