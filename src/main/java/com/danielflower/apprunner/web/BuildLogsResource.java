package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import com.danielflower.apprunner.mgmt.AppDescription;
import com.danielflower.apprunner.problems.AppNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

@Path("/v1/logs")
public class BuildLogsResource {
    public static final Logger log = LoggerFactory.getLogger(BuildLogsResource.class);

    private final AppEstate estate;

    public BuildLogsResource(AppEstate estate) {
        this.estate = estate;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}")
    public String logs(@PathParam("name") String name) {
        Optional<AppDescription> namedApp = estate.all().filter(desc -> desc.name().equals(name)).findFirst();

        if (namedApp.isPresent())
            return namedApp.get().latestBuildLog();

        throw new AppNotFoundException("No app found with name '" + name + "'. Valid names: " + estate.allAppNames());
    }
}
