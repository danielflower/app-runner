package e2e;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scaffolding.Photocopier;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.danielflower.apprunner.FileSandbox.fullPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZipSamplesTask {
    public static final Logger log = LoggerFactory.getLogger(ZipSamplesTask.class);

    @Test
    public void zipTheSamplesAndPutThemInTheResourcesDir() throws IOException {
        File outputDir = new File("src/main/resources/sample-apps");
        if (!outputDir.isDirectory()) {
            throw new RuntimeException("Expected sample app dir at " + fullPath(outputDir));
        }
        for (File file : Photocopier.sampleDir().listFiles(File::isDirectory)) {
            File outputFile = new File(outputDir, file.getName() + ".zip");
            assertThat(!outputFile.exists() || outputFile.delete(), is(true));
            zipDirectory(file, outputFile);
            log.info("Created " + fullPath(outputFile));
        }
    }

    public static void zipDirectory(File dir, File zipFile) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(zipFile);
             ZipOutputStream zout = new ZipOutputStream(fout)) {
            zipSubDirectory("", dir, zout);
        }
    }

    private static void zipSubDirectory(String basePath, File dir, ZipOutputStream zout) throws IOException {
        byte[] buffer = new byte[4096];
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String path = basePath + file.getName() + "/";
                zout.putNextEntry(new ZipEntry(path));
                zipSubDirectory(path, file, zout);
                zout.closeEntry();
            } else {
                try (FileInputStream fin = new FileInputStream(file)) {
                    zout.putNextEntry(new ZipEntry(basePath + file.getName()));
                    int length;
                    while ((length = fin.read(buffer)) > 0) {
                        zout.write(buffer, 0, length);
                    }
                    zout.closeEntry();
                }
            }
        }
    }

}
