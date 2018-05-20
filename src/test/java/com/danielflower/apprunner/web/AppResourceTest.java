package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import com.danielflower.apprunner.mgmt.SystemInfo;
import com.danielflower.apprunner.runners.AppRunnerFactoryProvider;
import com.danielflower.apprunner.runners.MavenRunnerFactory;
import com.danielflower.apprunner.web.v1.AppResource;
import org.apache.commons.io.output.NullOutputStream;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import scaffolding.AppRepo;
import scaffolding.MockAppDescription;
import scaffolding.TestConfig;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static com.danielflower.apprunner.web.WebServerTest.fileSandbox;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AppResourceTest {

    private final MockAppDescription myApp = new MockAppDescription("my-app", "git://something/.git");
    private final MockAppDescription anApp = new MockAppDescription("an-app", "git://something/.git");
    private final AppEstate estate = new AppEstate(new ProxyMap(), fileSandbox(), new AppRunnerFactoryProvider(Collections.singletonList(MavenRunnerFactory.createIfAvailable(TestConfig.config).get())));
    private final SystemInfo systemInfo = SystemInfo.create();
    private final AppResource appResource = new AppResource(estate, systemInfo, fileSandbox());

    @Test
    public void gettingAppsReturnsJsonObjectWithAppArrayOrderedByName() throws Exception {
        estate.add(myApp);
        estate.add(anApp);

        String json = appResource.apps(new MockUriInfo("http://localhost:1234/api/v1/apps"));
        JSONAssert.assertEquals("{apps:[ " +
            "{ name: \"an-app\", url: \"http://localhost:1234/an-app/\", " +
            "buildLogUrl: \"http://localhost:1234/api/v1/apps/an-app/build.log\", " +
            "consoleLogUrl: \"http://localhost:1234/api/v1/apps/an-app/console.log\", " +
            "gitUrl: \"git://something/.git\" }," +
            "{ name: \"my-app\", url: \"http://localhost:1234/my-app/\", gitUrl: \"git://something/.git\" }" +
            "]}", json, JSONCompareMode.STRICT_ORDER);
    }

    @Test
    public void appsAreCreatedByPostingAndItImmediatelyBuilds() {
        AppRepo repo = AppRepo.create("maven");

        UriInfo uriInfo = new MockUriInfo("http://localhost:1234/api/v1/apps");
        Response response = appResource.create(uriInfo, repo.gitUrl(), null);
        assertThat(response.getStatus(), is(201));
        assertThat(response.getHeaderString("Location"), is("http://localhost:1234/api/v1/apps/maven"));

        long matched = estate.all().filter(a -> a.name().equals("maven") && a.gitUrl().equals(repo.gitUrl())).count();
        assertThat(matched, is(1L));
    }

    @Test
    public void creatingAnAppReturnsA400IfTheGitUrlIsNotAccessible() {
        AppRepo repo = AppRepo.create("maven");

        UriInfo uriInfo = new MockUriInfo("http://localhost:1234/api/v1/apps");
        String badUrl = repo.gitUrl() + "broken";
        Response response = appResource.create(uriInfo, badUrl, "maven");
        assertThat(response.getStatus(), is(400));
        JSONAssert.assertEquals("{message: 'Could not clone git repository: Invalid remote: origin', gitUrl: '" + badUrl + "'}", (String) response.getEntity(), JSONCompareMode.LENIENT);
    }

    @Test
    public void creatingAnAppReturnsA501IfTheThereIsNoSuitableRunnerForTheRepo() throws IOException, GitAPIException {
        AppRepo repo = AppRepo.create("nodejs"); // the resource only has a maven runner registered
        UriInfo uriInfo = new MockUriInfo("http://localhost:1234/api/v1/apps");
        Response response = appResource.create(uriInfo, repo.gitUrl(), null);
        assertThat(response.getStatus(), is(501));
        JSONAssert.assertEquals("{message: 'No suitable runner found for this app', gitUrl: '" + repo.gitUrl() + "'}", (String) response.getEntity(), JSONCompareMode.LENIENT);
    }

    @Test
    public void updateCausesTheEstateUpdaterToRun() throws Exception {
        estate.add(myApp);
        estate.add(anApp);

        Response response = appResource.deploy(new MockUriInfo("http://localhost/blah"), "", "my-app");
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

        Response response = appResource.deploy(new MockUriInfo("http://localhost/blah"), "", "unreal-app");
        StreamingOutput stream = (StreamingOutput) response.getEntity();
        try {
            stream.write(new NullOutputStream());
            Assert.fail("Expected exception");
        } catch (WebApplicationException e) {
            assertThat(e.getResponse().getStatus(), is(404));
            assertThat(e.getResponse().getEntity(), is("No app found with name 'unreal-app'. Valid names: an-app, my-app"));
        }
    }

    private static class MockUriInfo implements UriInfo {

        private String uri;

        private MockUriInfo(String uri) {
            this.uri = uri;
        }

        public String getPath() {
            return null;
        }

        public String getPath(boolean decode) {
            return null;
        }

        public List<PathSegment> getPathSegments() {
            return null;
        }

        public List<PathSegment> getPathSegments(boolean decode) {
            return null;
        }

        public URI getRequestUri() {
            return URI.create(uri);
        }

        public UriBuilder getRequestUriBuilder() {
            return null;
        }

        public URI getAbsolutePath() {
            return null;
        }

        public UriBuilder getAbsolutePathBuilder() {
            return null;
        }

        public URI getBaseUri() {
            return null;
        }

        public UriBuilder getBaseUriBuilder() {
            return null;
        }

        public MultivaluedMap<String, String> getPathParameters() {
            return null;
        }

        public MultivaluedMap<String, String> getPathParameters(boolean decode) {
            return null;
        }

        public MultivaluedMap<String, String> getQueryParameters() {
            return null;
        }

        public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
            return null;
        }

        public List<String> getMatchedURIs() {
            return null;
        }

        public List<String> getMatchedURIs(boolean decode) {
            return null;
        }

        public List<Object> getMatchedResources() {
            return null;
        }

        public URI resolve(URI uri) {
            return null;
        }

        public URI relativize(URI uri) {
            return null;
        }
    }
}
