package scaffolding;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;

import java.io.File;

import static com.danielflower.apprunner.FileSandbox.dirPath;

public class DirectoryExistsMatcher extends CustomTypeSafeMatcher<File> {
    public DirectoryExistsMatcher() {
        super("directory exists");
    }

    @Override
    protected boolean matchesSafely(File item) {
        return item.isDirectory();
    }

    @Override
    protected void describeMismatchSafely(File item, Description mismatchDescription) {
        mismatchDescription.appendText(dirPath(item) + " " + (item.isDirectory() ? "exists" : "does not exist"));
    }

    public static DirectoryExistsMatcher directoryExists() {
        return new DirectoryExistsMatcher();
    }
}
