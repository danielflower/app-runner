@echo off
java -Dlogback.configurationFile=logback.xml -jar ..\target\app-runner-1.0-SNAPSHOT.jar config.properties
