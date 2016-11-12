App Runner
----------

The little web app runner that hosts java, clojure, scala and nodejs apps. This allows
you to easily deploy a self-hosted Platform As A Service where you can very easily add
new apps and have App Runner build and run them by cloning the code from a Git repo.

### Features

* Host your own Platform as a Service: you just need Java 8, plus optional build tools
(Maven, Leinigen, Scala/SBT, NodeJS/NPM).
* Deploy web apps with no build servers or deploy scripts needed: tell AppRunner the Git
URL and it will automatically build and host it.
* Auto deploy on source control change when using post-commit hooks
* Zero downtime deployment: when changes are being deployed, a new instance is built, tests
are run, and the app is started. Only when the new instance is running will it be made live.
* An [optional dashboard](https://github.com/danielflower/app-runner-home) which links to
all your apps making it easy to find, add, and deploy.
* Horizontally scale individual app runners across multiple machines with
[App Runner Router](https://github.com/danielflower/app-runner-router)

### Who is this for?

App Runner is especially useful for people or teams who are creating many little web applications
and want the convenience of a platform such as Heroku but cannot use an external service.

Change log
----------

* **1.2.0** Support for HTTPS (see the sample config file for more info). This is also
the first version that HTTPS can be used on [the app runner router](https://github.com/danielflower/app-runner-router)
(or other reverse proxy).
* **1.1.0** Optional support for Scala and better reporting of versions of tools such
as java, node, lein etc. When adding a new app, it is immediately cloned and an error
is returned if it cannot be cloned or this instance does not support the project type.
* **1.0.7** Optional backup support: specify a Git URL in the config (`appserver.backup.url`) 
to have the data directory backed up to a git repo once an hour.

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
* Scala and SBT

It's easiest if each tool is available to run from the path, but you can point to specific
paths by setting paths in your config file.

Download the latest version of _App Runner_ from [Maven central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22app-runner%22)

Run with `java -jar app-runner-{version}.jar /path/to/config.properties`

See `sample-config.properties` for sample configuration. The `local` directory in this repo also
has sample start scripts and logging configuration.

[![Build Status](https://travis-ci.org/danielflower/app-runner.svg?branch=master)](https://travis-ci.org/danielflower/app-runner)
