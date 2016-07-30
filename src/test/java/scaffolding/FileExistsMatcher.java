package scaffolding;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;

import java.io.File;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class FileExistsMatcher extends CustomTypeSafeMatcher<File> {
    public FileExistsMatcher() {
        super("file exists");
    }

    @Override
    protected boolean matchesSafely(File item) {
        return item.isFile();
    }

    @Override
    protected void describeMismatchSafely(File item, Description mismatchDescription) {
        mismatchDescription.appendText(dirPath(item) + " " + (item.isFile() ? "exists" : "does not exist"));
    }

    public static FileExistsMatcher fileExists() {
        return new FileExistsMatcher();
    }
}
