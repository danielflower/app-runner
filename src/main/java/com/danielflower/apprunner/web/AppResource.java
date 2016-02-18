package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import com.danielflower.apprunner.mgmt.AppDescription;
import com.danielflower.apprunner.problems.AppNotFoundException;
import com.danielflower.apprunner.runners.OutputToWriterBridge;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

@Path("/v1/apps")
public class AppResource {
    public static final Logger log = LoggerFactory.getLogger(AppResource.class);

    private final AppEstate estate;

    public AppResource(AppEstate estate) {
        this.estate = estate;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String apps() {
        JSONObject result = new JSONObject();
        List<JSONObject> apps = new ArrayList<>();
        estate.all()
            .sorted((o1, o2) -> o1.name().compareTo(o2.name()))
            .forEach(d -> apps.add(
                new JSONObject().put("name", d.name()).put("gitUrl", d.gitUrl())));
        result.put("apps", apps);
        return result.toString();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response create(@Context UriInfo uriInfo, @FormParam("gitUrl") String gitUrl) {
        log.info("Received request to create " + gitUrl);
        if (StringUtils.isBlank(gitUrl)) {
            return Response.status(400).entity("No gitUrl was specified").build();
        }

        try {
            AppDescription added = estate.addApp(gitUrl);
            return Response.status(201)
                .header("Location", uriInfo.getRequestUri() + "/" + added.name()).build();
        } catch (Exception e) {
            log.error("Error while adding app", e);
            return Response.serverError().entity("Error while adding app: " + e.getMessage()).build();
        }
    }


    @POST /* Maybe should be PUT, but too many hooks only use POST */
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}")
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
