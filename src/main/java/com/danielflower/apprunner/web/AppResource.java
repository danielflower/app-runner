package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
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

    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}")
    public Response update(@PathParam("name") String name) {
        StreamingOutput stream  = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(output))) {
                    log.info("Going to update " + name);
                    try {
                        estate.update(name, writer);
                        log.info("Finished updating " + name);
                        output.flush();
                        output.close();
                    } catch (IllegalArgumentException e) {
                        Response r = Response.status(404).entity(e.getMessage()).build();
                        throw new WebApplicationException(r);
                    } catch (IOException io) {
                        throw io;
                    } catch (Exception e) {
                        log.error("Error while updating " + name, e);
                    }
                }
            }
        };
        return Response.ok(stream).build();
    }

}
