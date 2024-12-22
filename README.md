App Runner
----------

A self-hosted platform-as-a-service that hosts web apps written in Java, Clojure, NodeJS, Python, golang, Scala and .NET Core.
Designed to be simple to deploy behind a firewall, on corporate intranets or at home. 
Once running, tell App Runner the Git URL of a web app and it will automatically build and host it for you, with
support to auto-deploy on every git push.

### Features

* Host your own Platform as a Service: you just need Java 11, plus optional build tools
(Maven, Leinigen, Scala/SBT, go compiler, NodeJS/NPM, Gradle, Python 2 or 3, .NET Core SDK).
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

* **2.3.0** Breaking golang change: it now uses go modules. Also, the default branch no longer needs to be called `master`
* **2.2.0** Bug fixes, particularly the one where apps were sometimes not shutting down.
* **2.1.0** Added [let's encrypt](https://letsencrypt.org) (and other ACME server) support for free HTTPS certs (see `sample-config.properties` for instructions)
* **1.6.0** Added `GET`/`POST`/`DELETE` `/api/v1/apps/{name}/data` endpoints to manipulate an app's data directory.
* **1.5.6** Added backup info to the apps API and added app name validation
* **1.5.4** Added optional config for handling proxy timeouts: `apprunner.proxy.idle.timeout` (default 30000ms) `apprunner.proxy.total.timeout` (default 60000ms).
* **1.5.1** Fixed bug where sometimes creating a new app it would say there are no suitable runners, even though there are.
Also added `lastBuild` and `lastSuccessfulBuild` to the `app` API to provide information about when and what
was built (the git commit info is included). 
* **1.4.0** Added a PUT `/apps/{name}` method to change the GIT URL of an app, and made it so `POST`ing 
an app to `/apps` that already exists returns a 400 error. Also fixed the return type (to `application/json`) for 
`POST /apps`. There is also `--app-name=your-app-name` passed as a command line parameter to Node apps, mostly to aid
in finding which app is which when looking at running processes.
* **1.3.4** GoLang support
* **1.2.2** Support for HTTPS (see the sample config file for more info). This is also
the first version that HTTPS can be used on [the app runner router](https://github.com/danielflower/app-runner-router)
(or other reverse proxies).
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

You need to have a Windows or Linux server available with Java 8 or later and one or more build tools
installed. One or more of Maven, Gradle, Leiningen, NodeJS with NPM, Scala with SBT, GoLang, Python 2 or 3.

It's easiest if each tool is available to run from the path, but you can point to specific
paths by setting paths in your config file.

Download the latest version of _App Runner_ from [Maven central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22app-runner%22)

Run with `java -jar app-runner-{version}.jar /path/to/config.properties`

See `sample-config.properties` for sample configuration. The `local` directory in this repo also
has sample start scripts and logging configuration.

[![Build Status](https://travis-ci.org/danielflower/app-runner.svg?branch=master)](https://travis-ci.org/danielflower/app-runner)

## Contribute

You will also need to provide `M2_HOME=/path/to/maven/home` before you run source code and test cases.
