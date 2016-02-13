package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.MockAppDescription;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AppResourceTest {

    private final MockAppDescription myApp = new MockAppDescription("my-app", "git://something/.git");
    private final MockAppDescription anApp = new MockAppDescription("an-app", "git://something/.git");
    AppEstate estate = new AppEstate();
    AppResource appResource = new AppResource(estate);

    @Test
    public void gettingAppsReturnsJsonObjectWithAppArrayOrderedByName() throws Exception {
        estate.add(myApp);
        estate.add(anApp);

        String json = appResource.apps();
        JSONAssert.assertEquals("{apps:[ " +
            "{ name: \"an-app\", gitUrl: \"git://something/.git\" }," +
            "{ name: \"my-app\", gitUrl: \"git://something/.git\" }" +
            "]}", json, JSONCompareMode.STRICT_ORDER);
    }

    @Test
    public void updateCausesTheEstateUpdaterToRun() throws Exception {
        estate.add(myApp);
        estate.add(anApp);

        Response response = appResource.update("my-app");
        StreamingOutput stream = (StreamingOutput) response.getEntity();
        stream.write(new NullOutputStream());
        assertThat(response.getStatus(), is(200));
        assertThat(myApp.updateCount, is(1));
        assertThat(anApp.updateCount, is(0));
    }

    @Test
    public void updateThrowsA404IfTheNameIsNotRecognisedAndShowsWhichAreValidNames() throws Exception {
        estate.add(myApp);
        estate.add(anApp);

        Response response = appResource.update("unreal-app");
        StreamingOutput stream = (StreamingOutput) response.getEntity();
        try {
            stream.write(new NullOutputStream());
            Assert.fail("Expected exception");
        } catch (WebApplicationException e) {
            assertThat(e.getResponse().getStatus(), is(404));
            assertThat(e.getResponse().getEntity(), is("No app found with name 'unreal-app'. Valid names: an-app, my-app"));
        }
    }
}
