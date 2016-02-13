package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import org.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/v1/apps")
public class AppResource {

    private final AppEstate estate;

    public AppResource(AppEstate estate) {
        this.estate = estate;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public String apps() {
        JSONObject result = new JSONObject();
        List<JSONObject> apps = new ArrayList<>();
        result.put("apps", apps);
        return result.toString();
    }

}
