package scaffolding;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.util.Fields;

public class RestClient {

    public static RestClient create(String appRunnerUrl) {
        HttpClient c = new HttpClient();
        try {
            c.start();
            return new RestClient(c, appRunnerUrl);

        } catch (Exception e) {
            throw new RuntimeException("Unable to make client", e);
        }
    }

    private final HttpClient client;
    private final String appRunnerUrl;

    private RestClient(HttpClient client, String appRunnerUrl) {
        this.client = client;
        this.appRunnerUrl = appRunnerUrl;
    }

    public ContentResponse createApp(String gitUrl) throws Exception {
        return client.POST(appRunnerUrl + "/api/v1/apps")
            .content(new FormContentProvider(new Fields() {{
                add("gitUrl", gitUrl);
            }})).send();
    }

    public ContentResponse deploy(String app) throws Exception {
        return client.POST(appRunnerUrl + "/api/v1/apps/" + app + "/deploy")
            .header("Accept", "application/json") // to simulate products like the Stash commit hook
            .send();
    }

    public ContentResponse stop(String app) throws Exception {
        return client.newRequest(appRunnerUrl + "/api/v1/apps/" + app + "/stop").method("PUT").send();
    }

    public ContentResponse homepage(String appName) throws Exception {
        return client.GET(appRunnerUrl + "/" + appName + "/");
    }

    public ContentResponse get(String url) throws Exception {
        return client.GET(url);
    }

    public void stop() {
        try {
            client.stop();
        } catch (Exception e) {
            // ignore
        }
    }

}
