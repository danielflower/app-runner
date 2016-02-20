package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import com.danielflower.apprunner.mgmt.AppDescription;
import com.danielflower.apprunner.mgmt.AppManager;
import com.danielflower.apprunner.problems.AppNotFoundException;
import com.danielflower.apprunner.runners.OutputToWriterBridge;
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
        return result.toString();
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response app(@Context UriInfo uriInfo, @PathParam("name") String name) {
        Optional<AppDescription> app = estate.app(name);
        if (app.isPresent()) {
            return Response.ok(appJson(uriInfo.getRequestUri(), app.get()).toString()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}/build.log")
    public String logs(@PathParam("name") String name) {
        Optional<AppDescription> namedApp = estate.app(name);
        if (namedApp.isPresent())
            return namedApp.get().latestBuildLog();
        throw new AppNotFoundException("No app found with name '" + name + "'. Valid names: " + estate.allAppNames());
    }

    public static JSONObject appJson(URI uri, AppDescription app) {
        URI restURI = uri.resolve("/api/v1/");

        return new JSONObject()
            .put("name", app.name())
            .put("buildLogUrl", restURI.resolve("apps/" + app.name() + "/build.log"))
            .put("consoleLogUrl", restURI.resolve("apps/" + app.name() + "/console.log"))
            .put("url", uri.resolve("/" + app.name() + "/"))
            .put("gitUrl", app.gitUrl());
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
            AppDescription added = estate.addApp(gitUrl, appName);
            return Response.status(201)
                .header("Location", uriInfo.getRequestUri() + "/" + added.name()).build();
        } catch (Exception e) {
            log.error("Error while adding app", e);
            return Response.serverError().entity("Error while adding app: " + e.getMessage()).build();
        }
    }


    @POST /* Maybe should be PUT, but too many hooks only use POST */
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}/deploy")
    public Response update(@PathParam("name") String name) {
        StreamingOutput stream = new UpdateStreamer(name);
        return Response.ok(stream).build();
    }

    private class UpdateStreamer implements StreamingOutput {
        private final String name;

        public UpdateStreamer(String name) {
            this.name = name;
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
//            Writer fileWriter = new FileWriter("build.log");
//            Writer compositeWriter = new CompositeWriter();

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
}
