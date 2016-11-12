package com.danielflower.apprunner.web.v1;

import com.danielflower.apprunner.mgmt.SystemInfo;
import com.danielflower.apprunner.runners.AppRunnerFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Api(value = "System")
@Path("/system")
public class SystemResource {
    public static final Logger log = LoggerFactory.getLogger(SystemResource.class);
    private final SystemInfo systemInfo;

    private final AtomicBoolean startupComplete;
    private final List<AppRunnerFactory> factories;

    public SystemResource(SystemInfo systemInfo, AtomicBoolean startupComplete, List<AppRunnerFactory> factories) {
        this.systemInfo = systemInfo;
        this.startupComplete = startupComplete;
        this.factories = factories;
    }

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Returns information about AppRunner, including information about sample apps")
    public Response systemInfo(@Context UriInfo uri) throws IOException {
        JSONObject result = new JSONObject();
        result.put("appRunnerStarted", startupComplete.get());
        result.put("host", systemInfo.hostName);
        result.put("user", systemInfo.user);

        JSONArray apps = new JSONArray();
        result.put("samples", apps);
        for (AppRunnerFactory factory : factories) {
            JSONObject sample = new JSONObject();
            sample.put("id", factory.id());
            sample.put("name", factory.id()); // for backwards compatibility
            sample.put("description", factory.description());
            sample.put("url", uri.getRequestUri().resolve("system/samples/" + factory.sampleProjectName()));
            sample.put("runCommands", new JSONArray(factory.startCommands()));
            sample.put("version", factory.versionInfo());
            apps.put(sample);
        }


        JSONObject os = new JSONObject();
        result.put("os", os);
        os.put("osName", systemInfo.osName);
        os.put("numCpus", systemInfo.numCpus);
        os.put("uptimeInSeconds", systemInfo.uptimeInMillis()  / 1000L);
        os.put("appRunnerPid", systemInfo.pid);

        JSONArray keys = new JSONArray();
        systemInfo.publicKeys.forEach(keys::put);
        result.put("publicKeys", keys);

        return Response.ok(result.toString(4)).build();
    }

    @GET
    @Path("/samples/{name}")
    @Produces("application/zip")
    @ApiOperation("Returns a ZIP file containing a sample app")
    public Response samples(@ApiParam(required = true, allowableValues = "maven.zip, lein.zip, nodejs.zip, sbt.zip") @PathParam("name") String name) throws IOException {
        List<String> names = factories.stream().map(AppRunnerFactory::sampleProjectName).collect(Collectors.toList());
        if (!names.contains(name)) {
            return Response.status(404).entity("Invalid sample app name. Valid names: " + names).build();
        }

        try (InputStream zipStream = getClass().getResourceAsStream("/sample-apps/" + name)) {
            return Response.ok(IOUtils.toByteArray(zipStream))
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                .build();
        }
    }

}
