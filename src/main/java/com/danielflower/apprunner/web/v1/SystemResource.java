package com.danielflower.apprunner.web.v1;

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
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Api(value = "System")
@Path("/system")
public class SystemResource {
    public static final Logger log = LoggerFactory.getLogger(SystemResource.class);
    static final String HOST_NAME = System.getenv("COMPUTERNAME");
    private static final Long pid;

    private final AtomicBoolean startupComplete;
    private final List<AppRunnerFactory> factories;

    static {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        pid = Pattern.matches("[0-9]+@.*", name) ? Long.parseLong(name.substring(0, name.indexOf('@'))) : null;
    }

    public SystemResource(AtomicBoolean startupComplete, List<AppRunnerFactory> factories) {
        this.startupComplete = startupComplete;
        this.factories = factories;
    }

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Returns information about AppRunner, including information about sample apps")
    public Response systemInfo(@Context UriInfo uri) throws IOException {
        JSONObject result = new JSONObject();
        result.put("appRunnerStarted", startupComplete.get());
        result.put("host", HOST_NAME);
        result.put("user", System.getProperty("user.name"));

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

        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        JSONObject os = new JSONObject();
        result.put("os", os);
        os.put("osName", System.getProperty("os.name"));
        os.put("numCpus", runtime.availableProcessors());
        os.put("uptimeInSeconds", runtimeMXBean.getUptime() / 1000);
        if (pid != null) {
            os.put("appRunnerPid", pid.longValue());
        }

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
