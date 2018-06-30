package com.danielflower.apprunner.web.v1;

import com.danielflower.apprunner.mgmt.BackupService;
import com.danielflower.apprunner.mgmt.SystemInfo;
import com.danielflower.apprunner.runners.AppRunnerFactory;
import io.muserver.rest.Description;
import io.muserver.rest.Required;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Description(value = "System")
@Path("/system")
public class SystemResource {
    public static final Logger log = LoggerFactory.getLogger(SystemResource.class);
    private final SystemInfo systemInfo;

    private final AtomicBoolean startupComplete;
    private final List<AppRunnerFactory> factories;
    private final BackupService backupService;
    private final String appRunnerVersion = ObjectUtils.firstNonNull(SystemResource.class.getPackage().getImplementationVersion(), "master");

    public SystemResource(SystemInfo systemInfo, AtomicBoolean startupComplete, List<AppRunnerFactory> factories, BackupService backupService) {
        this.systemInfo = systemInfo;
        this.startupComplete = startupComplete;
        this.factories = factories;
        this.backupService = backupService;
    }

    @GET
    @Produces("application/json")
    @Description(value = "Returns information about AppRunner, including information about sample apps")
    public Response systemInfo(@Context UriInfo uri) {
        JSONObject result = new JSONObject();
        result.put("appRunnerStarted", startupComplete.get());
        result.put("appRunnerVersion", appRunnerVersion);
        result.put("host", systemInfo.hostName);
        result.put("user", systemInfo.user);

        if (backupService != null) {
            JSONObject backupJson = new JSONObject()
                .put("backupUrl", backupService.remoteUri);
            Instant lastBackup = backupService.lastSuccessfulBackupTime;
            if (lastBackup != null) {
                backupJson.put("lastSuccessfulBackup", lastBackup.toString());
            }
            backupService.lastRunError().ifPresent(e -> backupJson.put("lastBackupError", e.getMessage()));
            result.put("backupInfo", backupJson);
        }

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
    @Description("Returns a ZIP file containing a sample app")
    public Response samples(@Required @PathParam("name") String name) throws IOException {
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
