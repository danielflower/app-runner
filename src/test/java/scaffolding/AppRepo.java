package scaffolding;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;

import java.io.File;

import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

public class AppRepo {

    public final File originDir;
    public final Git origin;

    private AppRepo(File originDir, Git origin) {
        this.originDir = originDir;
        this.origin = origin;
    }


    public static AppRepo create(String name) {
        try {
            File originDir = copyTestProjectToTemporaryLocation(name);

            InitCommand initCommand = Git.init();
            initCommand.setDirectory(originDir);
            Git origin = initCommand.call();

            origin.add().addFilepattern(".").call();
            origin.commit().setMessage("Initial commit").call();

            return new AppRepo(originDir, origin);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating git repo", e);
        }
    }

    public String gitUrl() {
        return originDir.toURI().toString();
    }
}
