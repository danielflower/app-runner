package com.danielflower.apprunner.web;

import com.danielflower.apprunner.AppEstate;
import com.danielflower.apprunner.mgmt.AppManager;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class AppResourceTest {

    AppEstate estate;
    AppResource appResource = new AppResource(estate);

    @Test
    public void gettingAppsReturnsJsonObjectWithAppArray() throws Exception {
//        estate.add();

        String json = appResource.apps();
        JSONAssert.assertEquals("{apps:[ ]}", json, JSONCompareMode.LENIENT);



    }
}
