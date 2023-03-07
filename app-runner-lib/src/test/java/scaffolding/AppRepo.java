package scaffolding;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.File;

import static scaffolding.Photocopier.copySampleAppToTempDir;

public class AppRepo {

    public static AppRepo create(String name) {
        return create(name, name);
    }

    public static AppRepo create(String name, String sampleAppName) {
        try {
            File originDir = copySampleAppToTempDir(sampleAppName);

            InitCommand initCommand = Git.init();
            initCommand.setDirectory(originDir);
            Git origin = initCommand.call();

            origin.add().addFilepattern(".").call();
            origin.commit().setMessage("Initial commit")
                .setAuthor(new PersonIdent("Author Test", "author@email.com"))
                .call();

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
