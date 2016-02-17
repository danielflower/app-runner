App Runner
----------

The little web app runner that hosts java, clojure and nodejs apps.

Running locally
---------------

Run `com.danielflower.apprunner.RunLocal.main` from your IDE. This will use the settings in
`sample-config.properties`. Try POSTing `gitUrl=https://github.com/danielflower/app-runner`
as a form-encoded request to `/api/v1/apps` to download, build and run an app.

Configuration
-------------

See `sample-config.properties` for configuration information. Each setting can be specified
as an environment variable, a java system property, or in a properties file who's path is
specified as a command line argument.