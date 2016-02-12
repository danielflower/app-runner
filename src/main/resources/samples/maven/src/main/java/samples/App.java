package samples;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class App {
    public static final Logger log = LoggerFactory.getLogger(App.class);
    public static void main(String[] args) throws Exception {

        // When run from app-runner, you must use the port set in the environment variable web.port
        int port = Integer.parseInt(firstNonNull(System.getenv("web.port"), "8080"));
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        Server jettyServer = new Server(addr);
        jettyServer.setStopAtShutdown(true);

        HandlerList handlers = new HandlerList();
        // TODO: set your own handlers
        handlers.addHandler(resourceHandler());

        // you must serve everything from a directory named after your app
        ContextHandler ch = new ContextHandler();
        ch.setContextPath("/maven");
        ch.setHandler(handlers);
        jettyServer.setHandler(ch);

        jettyServer.start();

        log.info("Started app at http://localhost:" + port + ch.getContextPath());

        jettyServer.join();
    }

    private static Handler resourceHandler() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newClassPathResource("/web", false, false));
        resourceHandler.setWelcomeFiles(new String[] {"index.html"});
        resourceHandler.setMinMemoryMappedContentLength(-1);
        return resourceHandler;
    }

}