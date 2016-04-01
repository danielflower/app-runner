package com.danielflower.apprunner.web;

import io.swagger.config.Scanner;
import io.swagger.config.SwaggerConfig;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.Set;
import java.util.stream.Collectors;

class SwaggerDocs {

    static void registerSwaggerJsonResource(ResourceConfig rc) {
        new SwaggerContextService()
            .withSwaggerConfig(new SwaggerConfig() {
                public Swagger configure(Swagger swagger) {
                    Info info = new Info();
                    info.setTitle("App Runner");
                    info.setDescription("The REST API for App Runner which is used for registering apps, deploying apps, viewing logs etc.");
                    info.setVersion("1.0");
                    swagger.setInfo(info);
                    swagger.setBasePath("/api/v1");
                    return swagger;
                }

                public String getFilterClass() {
                    return null;
                }
            })
            .withScanner(new Scanner() {
                private boolean prettyPrint;

                public Set<Class<?>> classes() {
                    return rc.getInstances().stream().map(Object::getClass).collect(Collectors.toSet());
                }

                public boolean getPrettyPrint() {
                    return prettyPrint;
                }

                public void setPrettyPrint(boolean b) {
                    prettyPrint = b;
                }
            })
            .initConfig()
            .initScanner();

        rc.packages(ApiListingResource.class.getPackage().getName());
    }
}
