package com.danielflower.apprunner.web;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Arrays.asList;

@Path("/v1/system")
public class SystemResource {
    public static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    private static final List<String> sampleProjectNames = asList("maven.zip", "lein.zip", "nodejs.zip");


    @GET
    @Path("/")
    @Produces("application/json")
    public Response systemInfo(@Context UriInfo uri) throws IOException {
        JSONObject result = new JSONObject();
        JSONArray apps = new JSONArray();
        result.put("samples", apps);
        for (String name : sampleProjectNames) {
            JSONObject sample = new JSONObject();
            sample.put("name", name.replace(".zip", ""));
            sample.put("url", uri.getRequestUri().resolve("system/samples/" + name));
            apps.put(sample);
        }
        return Response.ok(result.toString()).build();
    }

    @GET
    @Path("/samples/{name}")
    @Produces("application/zip")
    public Response samples(@PathParam("name") String name) throws IOException {
        if (!sampleProjectNames.contains(name)) {
            return Response.status(404).entity("Invalid sample app name. Valid names: " + sampleProjectNames).build();
        }

        try (InputStream zipStream = getClass().getResourceAsStream("/sample-apps/" + name)) {
            return Response.ok(IOUtils.toByteArray(zipStream))
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                .build();
        }
    }
}
