package scaffolding;

import com.danielflower.apprunner.mgmt.AppDescription;

import java.io.Writer;

public class MockAppDescription implements AppDescription {
    private final String gitUrl;
    private final  String name;
    public int updateCount = 0;

    public MockAppDescription(String name, String gitUrl) {
        this.gitUrl = gitUrl;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String gitUrl() {
        return gitUrl;
    }

    @Override
    public void stopApp() {
    }

    @Override
    public void update(Writer writer) throws Exception {
        ++updateCount;
    }
}
