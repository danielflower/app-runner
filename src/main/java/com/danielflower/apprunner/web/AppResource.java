package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import com.danielflower.apprunner.mgmt.AppDescription;
import com.danielflower.apprunner.mgmt.AppManager;
import com.danielflower.apprunner.problems.AppNotFoundException;
import com.danielflower.apprunner.io.OutputToWriterBridge;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.io.WriterOutputStream;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Path("/v1/apps")
public class AppResource {
    public static final Logger log = LoggerFactory.getLogger(AppResource.class);

    private final AppEstate estate;

    public AppResource(AppEstate estate) {
        this.estate = estate;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String apps(@Context UriInfo uriInfo) {

        JSONObject result = new JSONObject();
        List<JSONObject> apps = new ArrayList<>();
        estate.all()
            .sorted((o1, o2) -> o1.name().compareTo(o2.name()))
            .forEach(d -> apps.add(
                appJson(uriInfo.getRequestUri(), d)));
        result.put("apps", apps);
        return result.toString(4);
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response app(@Context UriInfo uriInfo, @PathParam("name") String name) {
        Optional<AppDescription> app = estate.app(name);
        if (app.isPresent()) {
            return Response.ok(appJson(uriInfo.getRequestUri(), app.get()).toString(4)).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}/build.log")
    public String buildLogs(@PathParam("name") String name) {
        Optional<AppDescription> namedApp = estate.app(name);
        if (namedApp.isPresent())
            return namedApp.get().latestBuildLog();
        throw new AppNotFoundException("No app found with name '" + name + "'. Valid names: " + estate.allAppNames());
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}/console.log")
    public String consoleLogs(@PathParam("name") String name) {
        Optional<AppDescription> namedApp = estate.app(name);
        if (namedApp.isPresent())
            return namedApp.get().latestConsoleLog();
        throw new AppNotFoundException("No app found with name '" + name + "'. Valid names: " + estate.allAppNames());
    }

    public static JSONObject appJson(URI uri, AppDescription app) {
        URI restURI = uri.resolve("/api/v1/");

        return new JSONObject()
            .put("name", app.name())
            .put("buildLogUrl", appUrl(app, restURI, "build.log"))
            .put("consoleLogUrl", appUrl(app, restURI, "console.log"))
            .put("url", uri.resolve("/" + app.name() + "/"))
            .put("deployUrl", appUrl(app, restURI, "deploy"))
            .put("gitUrl", app.gitUrl());
    }

    public static URI appUrl(AppDescription app, URI restURI, String path) {
        return restURI.resolve("apps/" + app.name() + "/" + path);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response create(@Context UriInfo uriInfo, @FormParam("gitUrl") String gitUrl, @FormParam("appName") String appName) {
        log.info("Received request to create " + gitUrl);
        if (isBlank(gitUrl)) {
            return Response.status(400).entity("No gitUrl was specified").build();
        }

        try {
            appName = isBlank(appName) ? AppManager.nameFromUrl(gitUrl) : appName;

            AppDescription appDescription;
            int status;
            Optional<AppDescription> existing = estate.app(appName);
            if (existing.isPresent()) {
                appDescription = existing.get();
                estate.remove(appDescription);
                status = 200;
            } else {
                status = 201;
            }
            appDescription = estate.addApp(gitUrl, appName);
            return Response.status(status)
                .header("Location", uriInfo.getRequestUri() + "/" + appDescription.name())
                .entity(appJson(uriInfo.getRequestUri(), estate.app(appName).get()).toString(4))
                .build();
        } catch (Exception e) {
            log.error("Error while adding app", e);
            return Response.serverError().entity("Error while adding app: " + e.getMessage()).build();
        }
    }

    @POST /* Maybe should be PUT, but too many hooks only use POST */
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    @Path("/{name}/deploy")
    public Response deploy(@Context UriInfo uriInfo, @HeaderParam("Accept") String accept, @PathParam("name") String name) throws IOException {
        StreamingOutput stream = new UpdateStreamer(name);
        if (MediaType.APPLICATION_JSON.equals(accept)) {
            StringBuilderWriter output = new StringBuilderWriter();
            try (WriterOutputStream writer = new WriterOutputStream(output)) {
                stream.write(writer);
                return app(uriInfo, name);
            }
        } else {
            return Response.ok(stream).build();
        }
    }

    private class UpdateStreamer implements StreamingOutput {
        private final String name;

        public UpdateStreamer(String name) {
            this.name = name;
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            try (Writer writer = new OutputStreamWriter(output)) {
                writer.write("Going to build and deploy " + name + "\n");
                log.info("Going to update " + name);
                try {
                    estate.update(name, new OutputToWriterBridge(writer));
                    log.info("Finished updating " + name);
                    writer.write("Success\n");
                } catch (AppNotFoundException e) {
                    Response r = Response.status(404).entity(e.getMessage()).build();
                    throw new WebApplicationException(r);
                } catch (Exception e) {
                    log.error("Error while updating " + name, e);
                    writer.write("Error while updating: " + e);
                    if (e instanceof IOException) {
                        throw (IOException) e;
                    }
                }
            }
        }
    }

    @PUT
    @Path("/{name}/stop")
    public Response stop(@PathParam("name") String name) {
        Optional<AppDescription> app = estate.app(name);
        if (app.isPresent()) {
            try {
                log.info("Going to stop " + name);
                app.get().stopApp();
                return Response.ok().build();
            } catch (Exception e) {
                log.error("Couldn't stop app via REST call", e);
                return Response.serverError().entity(e.toString()).build();
            }
        } else {
            return Response.status(404).build();
        }
    }

}
