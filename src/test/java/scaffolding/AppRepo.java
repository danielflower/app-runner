package scaffolding;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;

import java.io.File;

import static com.danielflower.apprunner.FileSandbox.dirPath;
import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

public class AppRepo {

    public final File originDir;
    public final Git origin;
    private final File localDir;
    private final Git local;

    private AppRepo(File originDir, Git origin, File localDir, Git local) {
        this.originDir = originDir;
        this.origin = origin;
        this.localDir = localDir;
        this.local = local;
    }


    public static AppRepo create(String name) {
        try {
            File originDir = copyTestProjectToTemporaryLocation(name);

            InitCommand initCommand = Git.init();
            initCommand.setDirectory(originDir);
            Git origin = initCommand.call();

            origin.add().addFilepattern(".").call();
            origin.commit().setMessage("Initial commit").call();


            File localDir = Photocopier.folderForSampleProject(name);
            Git local = Git.cloneRepository()
                .setBare(false)
                .setDirectory(localDir)
                .setURI(originDir.toURI().toString())
                .call();

            return new AppRepo(originDir, origin, localDir, local);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating git repo", e);
        }
    }

    public String gitUrl() {
        return originDir.toURI().toString();
    }
}
