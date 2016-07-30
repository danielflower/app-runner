package com.danielflower.apprunner.mgmt;

import com.danielflower.apprunner.FileSandbox;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.junit.Before;
import org.junit.Test;
import scaffolding.Photocopier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static scaffolding.DirectoryExistsMatcher.directoryExists;
import static scaffolding.FileExistsMatcher.fileExists;

public class BackupServiceTest {
    private final File localDir = Photocopier.folderForSampleProject("local");
    private final File remoteDir = Photocopier.folderForSampleProject("remote");
    private URIish remoteUri;

    @Before
    public void setup() throws GitAPIException, MalformedURLException {
        Git.init().setDirectory(remoteDir).setBare(true).call();
        remoteUri = new URIish(remoteDir.toURI().toURL());
    }

    @Test
    public void backsUpFilesButNotReposOrTempDirs() throws Exception {
        BackupService backupService = BackupService.prepare(localDir, remoteUri);
        backupService.backup();

        write(localDir, "repos.properties", "app=url");

        backupService.backup();

        File backupCopyDir = Photocopier.folderForSampleProject("clone");
        Git backedCopyRepo = Git.cloneRepository().setDirectory(backupCopyDir).setBare(false).setURI(remoteDir.toURI().toString()).call();
        assertThat(read(backupCopyDir, "repos.properties"), equalTo("app=url"));

        FileSandbox sandbox = new FileSandbox(localDir);
        File tempDir = sandbox.tempDir("blah");
        write(tempDir, "temp.txt", "I should not be backed up");

        File appDataDir = sandbox.appDir("my-app", "data/repo/temp");
        write(appDataDir, "some.data", "Hello there");
        File appRepoDir = sandbox.appDir("my-app", "repo");
        Git.init().setDirectory(appRepoDir).setBare(false).call();
        write(appRepoDir, "some.data", "Hi from repo");

        backupService.backup();
        backedCopyRepo.pull().call();

        assertThat(new File(backupCopyDir, "temp"), not(directoryExists()));
        assertThat(new File(backupCopyDir, "apps/my-app/data/repo/temp/some.data"), fileExists());
        assertThat(new File(backupCopyDir, "apps/my-app/repo"), not(directoryExists()));

        backupService.backup();
    }

    @Test
    public void onlyCreatesNewGitRepoIfDataDirIsNotARepo() throws Exception {
        createStartBackupStop(localDir, remoteUri);
        createStartBackupStop(localDir, remoteUri);
    }
    private static void createStartBackupStop(File localDir, URIish remoteUri) throws Exception {
        BackupService service = BackupService.prepare(localDir, remoteUri);
        service.start();
        Thread.sleep(100);
        service.stop();
        assertThat(service.lastRunError(), equalTo(Optional.empty()));
    }

    @Test
    public void backupsWorkEvenWhenFilesAreBeingWrittenTo() throws Exception {
        BackupService backupService = BackupService.prepare(localDir, remoteUri);

        AtomicBoolean running = new AtomicBoolean(true);
        File file = new File(localDir, "something.txt");
        StringBuffer exception = new StringBuffer();
        StringBuffer expected = new StringBuffer();

        Thread thread = new Thread(() -> {
            try (FileWriter writer = new FileWriter(file)) {
                int count = 0;
                while (running.get()) {
                    String v = String.valueOf(count++);
                    writer.append(v);
                    writer.flush();
                    expected.append(v);
                }
            } catch (Exception e) {
                exception.append(e);
            }
        });
        thread.start();

        Thread.sleep(100);
        backupService.backup();
        Thread.sleep(100);
        backupService.backup();

        running.set(false);
        thread.join();
        backupService.backup();

        assertThat(exception.toString(), equalTo(""));

        File backupCopyDir = Photocopier.folderForSampleProject("clone");
        Git.cloneRepository().setDirectory(backupCopyDir).setBare(false).setURI(remoteDir.toURI().toString()).call();
        assertThat(read(backupCopyDir, "something.txt"), equalTo(expected.toString()));
    }

    private static void write(File dir, String path, String value) throws IOException {
        FileUtils.writeStringToFile(new File(dir, path), value, "UTF-8");
    }

    private static String read(File backupClone, String path) throws IOException {
        return FileUtils.readFileToString(new File(backupClone, path), "UTF-8");
    }

}
