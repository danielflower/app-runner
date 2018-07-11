package samples;

import io.muserver.ContentTypes;
import io.muserver.Method;
import io.muserver.MuServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.muserver.ContextHandlerBuilder.context;
import static io.muserver.MuServerBuilder.muServer;
import static io.muserver.handlers.ResourceHandlerBuilder.fileOrClasspath;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        Map<String, String> settings = System.getenv();

        // When run from app-runner, you must use the port set in the environment variable APP_PORT
        int port = Integer.parseInt(settings.getOrDefault("APP_PORT", "8081"));
        // All URLs must be prefixed with the app name, which is got via the APP_NAME env var.
        String appName = settings.getOrDefault("APP_NAME", "my-app");
        log.info("Starting " + appName + " on port " + port);

        MuServer server = muServer()
            .withHttpPort(port)
            .addHandler(
                context(appName)
                    .addHandler(Method.GET, "/hello/{name}", (req, resp, params) -> {
                        resp.contentType(ContentTypes.TEXT_HTML);
                        resp.write("Hello, <b>" + params.get("name") + "</b>!");
                    })
                    .addHandler(fileOrClasspath("src/main/resources/web", "/web"))
            )
            .start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        log.info("Started app at " + server.uri().resolve("/" + appName));
    }

}