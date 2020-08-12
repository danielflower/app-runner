package com.danielflower.apprunner.web.v1;

import com.danielflower.apprunner.AppEstate;
import com.danielflower.apprunner.FileSandbox;
import com.danielflower.apprunner.io.OutputToWriterBridge;
import com.danielflower.apprunner.io.Zippy;
import com.danielflower.apprunner.mgmt.*;
import com.danielflower.apprunner.problems.AppNotFoundException;
import com.danielflower.apprunner.runners.UnsupportedProjectTypeException;
import io.muserver.rest.ApiResponse;
import io.muserver.rest.Description;
import io.muserver.rest.Required;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.io.WriterOutputStream;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Description(value = "Application")
@Path("/apps")
public class AppResource {
    public static final Logger log = LoggerFactory.getLogger(AppResource.class);

    private final AppEstate estate;
    private final SystemInfo systemInfo;
    private final FileSandbox fileSandbox;

    public AppResource(AppEstate estate, SystemInfo systemInfo, FileSandbox fileSandbox) {
        this.estate = estate;
        this.systemInfo = systemInfo;
        this.fileSandbox = fileSandbox;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Gets all registered apps")
    public String apps(@Context UriInfo uriInfo) {
        JSONObject result = new JSONObject();
        List<JSONObject> apps = new ArrayList<>();
        estate.all()
            .sorted(Comparator.comparing(AppDescription::name))
            .forEach(d -> apps.add(
                appJson(uriInfo.getRequestUri(), d)));
        result.put("appCount", apps.size());
        result.put("apps", apps);
        return result.toString(4);
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Gets a single app")
    public Response app(@Context UriInfo uriInfo, @Required @Description(value = "The name of the app", example = "app-runner-home") @PathParam("name") String name) {
        Optional<AppDescription> app = estate.app(name);
        if (app.isPresent()) {
            return Response.ok(appJson(uriInfo.getRequestUri(), app.get()).toString(4)).type(MediaType.APPLICATION_JSON).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces("text/plain;charset=utf-8")
    @Path("/{name}/build.log")
    @Description(value = "Gets the latest build log as plain text for the given app")
    public String buildLogs(@Required @Description(value = "The name of the app", example = "app-runner-home") @PathParam("name") String name) {
        Optional<AppDescription> namedApp = estate.app(name);
        if (namedApp.isPresent())
            return namedApp.get().latestBuildLog();
        throw new AppNotFoundException("No app found with name '" + name + "'. Valid names: " + estate.allAppNames());
    }

    @GET
    @Produces("text/plain;charset=utf-8")
    @Path("/{name}/console.log")
    @Description(value = "Gets the latest console log as plain text for the given app")
    public String consoleLogs(@Required @Description(value = "The name of the app", example = "app-runner-home") @PathParam("name") String name) {
        Optional<AppDescription> namedApp = estate.app(name);
        if (namedApp.isPresent())
            return namedApp.get().latestConsoleLog();
        throw new AppNotFoundException("No app found with name '" + name + "'. Valid names: " + estate.allAppNames());
    }

    @GET
    @Produces("application/zip")
    @Path("/{name}/data")
    @Description(value = "Gets the contents of the app's data directory as a zip file")
    public StreamingOutput getAppData(@Required @Description(value = "The name of the app", example = "app-runner-home") @PathParam("name") String name) {
        AppDescription ad = getAppDescription(name);
        log.info("Getting data for " + name);
        return output -> Zippy.zipDirectory(ad.dataDir(), output);
    }

    private AppDescription getAppDescription(@Required @Description(value = "The name of the app", example = "app-runner-home") @PathParam("name") String name) {
        Optional<AppDescription> namedApp = estate.app(name);
        if (!namedApp.isPresent())
            throw new NotFoundException("No app with name " + name);
        return namedApp.get();
    }

    @DELETE
    @Path("/{name}/data")
    @Description(value = "Deletes all the files for an app")
    @ApiResponse(code = "204", message = "Files deleted successfully")
    @ApiResponse(code = "500", message = "At least one file could not be deleted")
    public Response deleteAppData(@Required @Description(value = "The name of the app") @PathParam("name") String name) throws IOException {
        AppDescription ad = getAppDescription(name);
        File[] children = ad.dataDir().listFiles();
        if (children == null) {
            return Response.serverError().entity("Could not access data dir").build();
        }
        for (File child : children) {
            log.info("Deleting " + child.getCanonicalPath());
            if (child.isFile()) {
                if (!child.delete()) {
                    return Response.serverError().entity("Could not delete " + child.getName()).build();
                }
            } else {
                FileUtils.deleteDirectory(child);
            }
        }
        return Response.noContent().build();
    }

    @POST
    @Consumes({"application/octet-stream", "application/zip"})
    @Path("/{name}/data")
    @Description(value = "Sets the contents of the app's data directory with the contents of the zip file")
    @ApiResponse(code = "204", message = "Files uploaded successfully")
    public Response setAppData(@Required @Description(value = "The name of the app", example = "app-runner-home") @PathParam("name") String name,
                               @Description("A zip file containing files that will be unzipped")
                               @Required InputStream requestBody) throws IOException {
        AppDescription ad = getAppDescription(name);
        if (ad.dataDir().listFiles().length > 0) {
            return Response.status(400).entity("File uploading is only supported for apps with empty data directories.").build();
        }

        log.info("Setting data for " + name);

        String dataDirPath = ad.dataDir().getCanonicalPath();
        File unzipTo = fileSandbox.tempDir("post-data-" + UUID.randomUUID().toString());
        String unzipToPath = unzipTo.getCanonicalPath();
        log.info("Going to unzip files to temp dir " + unzipToPath);

        int filesUnzipped = 0;
        try (ZipInputStream zis = new ZipInputStream(requestBody)) {
            ZipEntry nextEntry;
            while ((nextEntry = zis.getNextEntry()) != null) {
                filesUnzipped++;
                String destPath = FilenameUtils.concat(unzipToPath, nextEntry.getName());
                File dest = new File(destPath);
                if (nextEntry.isDirectory()) {
                    if (dest.mkdirs()) {
                        log.info("Created " + destPath);
                    } else {
                        log.warn("Failed to create " + destPath);
                    }
                } else {
                    if (dest.getParentFile().mkdirs()) {
                        log.info("Created " + dest.getParentFile());
                    }
                    log.info("Unzipping " + (nextEntry.getName()));
                    try (FileOutputStream fos = new FileOutputStream(dest, false)) {
                        IOUtils.copy(zis, fos);
                    }
                }
            }
        }

        if (filesUnzipped > 0) {
            log.info("Going to move temp dir to app data path " + ad.dataDir().getCanonicalPath());
            if (!ad.dataDir().delete()) {
                log.warn("Could not delete old data dir");
            }
            FileUtils.moveDirectory(unzipTo, new File(dataDirPath));
        }
        return Response.status(204).build();
    }

    private JSONObject appJson(URI uri, AppDescription app) {
        URI restURI = uri.resolve("/api/v1/");

        Availability availability = app.currentAvailability();
        BuildStatus lastBuildStatus = app.lastBuildStatus();
        BuildStatus lastSuccessfulBuild = app.lastSuccessfulBuild();
        return new JSONObject()
            .put("name", app.name())
            .put("contributors", getContributorsList(app))
            .put("buildLogUrl", appUrl(app, restURI, "build.log"))
            .put("consoleLogUrl", appUrl(app, restURI, "console.log"))
            .put("url", uri.resolve("/" + app.name() + "/"))
            .put("deployUrl", appUrl(app, restURI, "deploy"))
            .put("available", availability.isAvailable)
            .put("availableStatus", availability.availabilityStatus)
            .put("lastBuild", lastBuildStatus.toJSON())
            .put("lastSuccessfulBuild", lastSuccessfulBuild == null ? null : lastSuccessfulBuild.toJSON())
            .put("gitUrl", app.gitUrl())
            .put("host", systemInfo.hostName);
    }

    private static String getContributorsList(AppDescription app) {
        String contributors = "";
        String[] contributorsArray = app.contributors().toArray(new String[0]);
        Arrays.sort(contributorsArray);
        for (String name : contributorsArray) {
            contributors += name + ", ";
        }
        if (contributors.length() > 2) {
            contributors = contributors.substring(0, contributors.length() - 2);
        }
        return contributors;
    }

    private static URI appUrl(AppDescription app, URI restURI, String path) {
        return restURI.resolve("apps/" + app.name() + "/" + path);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Description(value = "Registers a new app with AppRunner. Note that it does not deploy it.")
    @ApiResponse(code = "201", message = "The new app was successfully registered")
    @ApiResponse(code = "400", message = "The git URL was not specified or the git repo could not be cloned, or the app name is not valid.")
    @ApiResponse(code = "409", message = "There is already an app with that name")
    @ApiResponse(code = "501", message = "The app type is not supported by this apprunner")
    public Response create(@Context UriInfo uriInfo,
                           @Required @Description("An SSH or HTTP git URL that points to an app-runner compatible app")
                           @FormParam("gitUrl") String gitUrl,
                           @Description("The ID that the app will be referenced which should just be letters, numbers, and hyphens. Leave blank to infer it from the git URL")
                           @FormParam("appName") String appName) {
        log.info("Received request to create " + gitUrl);
        if (isBlank(gitUrl)) {
            return Response.status(400).entity(new JSONObject()
                .put("message", "No git URL was specified")
                .toString()).build();
        }

        appName = isBlank(appName) ? AppManager.nameFromUrl(gitUrl) : appName;

        Optional<AppDescription> existing = estate.app(appName);
        if (existing.isPresent()) {
            return Response.status(409).entity(new JSONObject()
                .put("message", "There is already an app with that ID")
                .toString()).build();
        }
        return responseForAddingAppToEstate(uriInfo, gitUrl, appName, 201);

    }

    private Response responseForAddingAppToEstate(UriInfo uriInfo, String gitUrl, String appName, int status) {
        AppDescription appDescription;
        try {
            appDescription = estate.addApp(gitUrl, appName);
            return Response.status(status)
                .header("Location", uriInfo.getRequestUri() + "/" + appDescription.name())
                .header("Content-Type", "application/json")
                .entity(appJson(uriInfo.getRequestUri(), estate.app(appName).get()).toString(4))
                .build();
        } catch (UnsupportedProjectTypeException e) {
            return Response.status(501)
                .header("Content-Type", "application/json")
                .entity(new JSONObject()
                    .put("message", "No suitable runner found for this app")
                    .put("gitUrl", gitUrl)
                    .toString(4))
                .build();
        } catch (GitAPIException e) {
            return Response.status(400)
                .header("Content-Type", "application/json")
                .entity(new JSONObject()
                    .put("message", "Could not clone git repository: " + e.getMessage())
                    .put("gitUrl", gitUrl)
                    .toString(4))
                .build();
        } catch (ValidationException ve) {
            return Response.status(400)
                .header("Content-Type", "application/json")
                .entity(new JSONObject()
                    .put("message", ve.getMessage())
                    .toString(4))
                .build();
        } catch (Exception e) {
            String errorId = "ERR" + System.currentTimeMillis();
            log.error("Error while adding app. ErrorID=" + errorId, e);
            return Response.serverError()
                .header("Content-Type", "application/json")
                .entity(new JSONObject()
                    .put("message", "Error while adding app")
                    .put("errorID", errorId)
                    .put("detailedError", e.toString())
                    .put("gitUrl", gitUrl)
                    .toString(4))
                .build();
        }
    }

    @PUT
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Description(value = "Updates the git URL of an existing app")
    @ApiResponse(code = "200", message = "Success - call deploy after this to build and deploy from the new URL")
    @ApiResponse(code = "400", message = "The name or git URL was not specified or the git repo could not be cloned")
    @ApiResponse(code = "404", message = "The app does not exist")
    @ApiResponse(code = "501", message = "The app type is not supported by this apprunner")
    public Response update(@Context UriInfo uriInfo,
                           @Required @Description(value = "An SSH or HTTP git URL that points to an app-runner compatible app")
                           @FormParam("gitUrl") String gitUrl,
                           @Description(value = "The ID of the app to update")
                           @PathParam("name") String appName) {
        log.info("Received request to update " + appName + " to " + gitUrl);
        if (isBlank(gitUrl) || isBlank(appName)) {
            return Response.status(400).entity(new JSONObject()
                .put("message", "No git URL was specified")
                .toString()).build();
        }
        Optional<AppDescription> existing = estate.app(appName);
        if (!existing.isPresent()) {
            return Response.status(404).entity(new JSONObject()
                .put("message", "No application called " + appName + " exists")
                .toString()).build();
        }
        AppDescription desc = existing.get();
        desc.gitUrl(gitUrl);

        return Response.status(200)
            .header("Location", uriInfo.getRequestUri() + "/" + desc.name())
            .header("Content-Type", "application/json")
            .entity(appJson(uriInfo.getRequestUri(), estate.app(appName).get()).toString(4))
            .build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{name}")
    @Description(value = "De-registers an application")
    public Response delete(@Context UriInfo uriInfo, @Description(value = "The name of the app") @PathParam("name") String name) throws IOException {
        Optional<AppDescription> existing = estate.app(name);
        if (existing.isPresent()) {
            AppDescription appDescription = existing.get();
            String entity = appJson(uriInfo.getRequestUri(), appDescription).toString(4);
            estate.remove(appDescription);
            return Response.ok(entity).build();
        } else {
            return Response.status(400).entity("Could not find app with name " + name).build();
        }
    }

    @POST /* Maybe should be PUT, but too many hooks only use POST */
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    @Path("/{name}/deploy")
    @Description(value = "Deploys an app", details = "Deploys the app by fetching the latest changes from git, building it, " +
        "starting it, polling for successful startup by making GET requests to /{name}/, and if it returns any HTTP response " +
        "it shuts down the old version of the app. If any steps before that fail, the old version of the app will continue serving " +
        "requests.")
    @ApiResponse(code = "200", message = "Returns 200 if the command was received successfully. Whether the build " +
        "actually succeeds or fails is ignored. Returns streamed plain text of the build log and console startup, unless the Accept" +
        " header includes 'application/json'.")
    public Response deploy(@Context UriInfo uriInfo, @Description(value = "The type of response desired, e.g. application/json or text/plain", example = "application/json") @HeaderParam("Accept") String accept,
                           @Required @Description(value = "The name of the app", example = "app-runner-home") @PathParam("name") String name) throws IOException {
        StreamingOutput stream = new UpdateStreamer(name);
        if (MediaType.APPLICATION_JSON.equals(accept)) {
            StringBuilderWriter output = new StringBuilderWriter();
            try (WriterOutputStream writer = new WriterOutputStream(output)) {
                stream.write(writer);
                return app(uriInfo, name);
            }
        } else {
            return Response.ok(stream).type("text/plain;charset=utf-8").build();
        }
    }

    private class UpdateStreamer implements StreamingOutput {
        private final String name;

        UpdateStreamer(String name) {
            this.name = name;
        }

        public void write(OutputStream output) throws IOException, WebApplicationException {
            try (Writer writer = new OutputStreamWriter(output)) {
                writer.write("Going to build and deploy " + name + " at " + new Date() + LINE_SEPARATOR);
                writer.flush();
                log.info("Going to update " + name);
                try {
                    estate.update(name, new OutputToWriterBridge(writer));
                    log.info("Finished updating " + name);
                    writer.write("Success" + LINE_SEPARATOR);
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
    @Description(value = "Stop an app from running, but does not de-register it. Call the deploy action to restart it.")
    public Response stop(@Description(value = "The app to stop") @PathParam("name") String name) {
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
