package com.danielflower.apprunner;

import com.danielflower.apprunner.mgmt.AppDescription;
import com.danielflower.apprunner.web.ProxyMap;
import org.junit.Before;
import org.junit.Test;
import scaffolding.MockAppDescription;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

import static com.danielflower.apprunner.web.WebServerTest.fileSandbox;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AppEstateTest {
    private AppEstate estate;

    @Before
    public void populate() throws IOException {
        estate = new AppEstate(new ProxyMap(), fileSandbox(), null, appRunnerHooks);
        estate.add(app("Y app"));
        estate.add(app("z app"));
        estate.add(app("c app"));
        estate.add(app("a app"));
        estate.add(app("B app"));
    }

    @Test
    public void startsAlphabetically() throws Exception {
        List<String> ordered = estate.appsByStartupOrder(null)
            .map(AppDescription::name)
            .collect(Collectors.toList());
        assertThat(ordered, equalTo(asList("a app", "B app", "c app", "Y app", "z app")));
    }

    @Test
    public void startsAlphabeticallyWithOptionalStartupAppFirst() throws Exception {
        List<String> ordered = estate.appsByStartupOrder("c app")
            .map(AppDescription::name)
            .collect(Collectors.toList());
        assertThat(ordered, equalTo(asList("c app", "a app", "B app", "Y app", "z app")));
    }

    private static MockAppDescription app(String name) throws UnsupportedEncodingException {
        return new MockAppDescription(name, "https://github.com/danielflower/" + URLEncoder.encode(name, "UTF-8"));
    }
}
