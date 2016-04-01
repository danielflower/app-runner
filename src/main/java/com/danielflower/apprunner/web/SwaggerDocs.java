package com.danielflower.apprunner.web;

import com.danielflower.apprunner.App;
import com.danielflower.apprunner.web.v1.AppResource;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.glassfish.jersey.server.ResourceConfig;

class SwaggerDocs {

    static ContextHandler buildSwaggerUI() throws Exception {
        final ResourceHandler swaggerUIResourceHandler = new ResourceHandler();
        swaggerUIResourceHandler.setResourceBase(App.class.getClassLoader().getResource("META-INF/resources/webjars/swagger-ui/2.1.4").toURI().toString());
        final ContextHandler swaggerUIContext = new ContextHandler();
        swaggerUIContext.setContextPath("/docs/");
        swaggerUIContext.setHandler(swaggerUIResourceHandler);
        return swaggerUIContext;
    }

    static void registerSwaggerJsonResource(ResourceConfig rc) {
        // Isn't this amazingly bad?!?! An instance of an object is created. Some setters are called, and
        // then the garbage collector will destroy the object that seemingly did nothing, and yet this is
        // presumably setting some global static values which are used by swagger. Furthermore, the order
        // of the setters matters. Things like description must be set before setScan is called.
        // What do people have against passing dependencies and config into constructors?
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0");
        beanConfig.setTitle("App Runner");
        beanConfig.setDescription("The REST API for App Runner which is used for registering apps, deploying apps, viewing logs etc.");
        beanConfig.setBasePath("/api/v1");
        beanConfig.setResourcePackage(AppResource.class.getPackage().getName());
        beanConfig.setScan(true);

        rc.packages(ApiListingResource.class.getPackage().getName());
    }
}
