Running locally
---------------

To test: `lein test`

To run the web server: `lein ring server`

To build and run it as AppRunner runs it:

    lein do test, uberjar
    java -jar target/leinapp-1.0-SNAPSHOT-standalone.jar
