App Runner
----------

The little web app runner that hosts java, clojure and nodejs apps.

Running locally
---------------

Run `com.danielflower.apprunner.RunLocal.main` from your IDE. This will use the settings in
`sample-config.properties`. Upon startup, it will try to download, build, and deploy the
application specified in the config. Launch the URL that is logged on startup see the hosted
sample app.

Deploying
---------

You need to have a Windows or Linux server available with Java and one or more build tools
installed:

* Java 8 or later
* Maven (if you wish to support Maven builds)
* Lein (if you wish to support Clojure builds)
* NodeJS and NPM (if you wish to support Nodejs builds)

It's easiest if each tool is available to run from the path, but you can point to specific
paths by setting paths in your config file.

Download the latest version of _App Runner_ from [Maven central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22app-runner%22)

Run with `java -jar app-runner-{version}.jar /path/to/config.properties`

See `sample-config.properties` for sample configuration. The `local` directory in this repo also
has sample start scripts and logging configuration.
