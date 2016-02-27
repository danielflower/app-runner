package scaffolding;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;

import java.io.File;

import static scaffolding.Photocopier.copySampleAppToTempDir;

public class AppRepo {

    public static AppRepo create(String name) {
        try {
            File originDir = copySampleAppToTempDir(name);

            InitCommand initCommand = Git.init();
            initCommand.setDirectory(originDir);
            Git origin = initCommand.call();

            origin.add().addFilepattern(".").call();
            origin.commit().setMessage("Initial commit").call();

            return new AppRepo(name, originDir, origin);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating git repo", e);
        }
    }

    public final String name;
    public final File originDir;
    public final Git origin;

    private AppRepo(String name, File originDir, Git origin) {
        this.name = name;
        this.originDir = originDir;
        this.origin = origin;
    }

    public String gitUrl() {
        return originDir.toURI().toString();
    }
}
