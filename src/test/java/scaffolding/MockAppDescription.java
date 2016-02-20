package scaffolding;

import com.danielflower.apprunner.mgmt.AppDescription;
import org.apache.maven.shared.invoker.InvocationOutputHandler;

public class MockAppDescription implements AppDescription {
    private final String gitUrl;
    private final  String name;
    public int updateCount = 0;

    public MockAppDescription(String name, String gitUrl) {
        this.gitUrl = gitUrl;
        this.name = name;
    }

    public String name() {
        return name;
    }

    public String gitUrl() {
        return gitUrl;
    }

    public String latestBuildLog() {
        return "";
    }

    @Override
    public String latestConsoleLog() {
        return "";
    }

    public void stopApp() {
    }

    public void update(InvocationOutputHandler outputHandler) throws Exception {
        ++updateCount;
    }
}
