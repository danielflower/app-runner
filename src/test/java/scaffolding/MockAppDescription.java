package scaffolding;

import com.danielflower.apprunner.mgmt.AppDescription;
import com.danielflower.apprunner.runners.RunnerProvider;
import org.apache.maven.shared.invoker.InvocationOutputHandler;

import java.util.ArrayList;

public class MockAppDescription implements AppDescription {
    private final String gitUrl;
    private final  String name;
    public int updateCount = 0;
    private ArrayList<String> contributors;

    public MockAppDescription(String name, String gitUrl) {
        this.gitUrl = gitUrl;
        this.name = name;
        this.contributors = new ArrayList<>();
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

    public String latestConsoleLog() {
        return "";
    }

    public ArrayList<String> contributors() {
        return contributors;
    }

    public void stopApp() {
    }

    public void update(RunnerProvider runnerProvider, InvocationOutputHandler outputHandler) throws Exception {
        ++updateCount;
    }
}
