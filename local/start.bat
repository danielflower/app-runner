@echo off
java -Dlogback.configurationFile=logback.xml -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -jar ..\target\app-runner-1.2-SNAPSHOT.jar config.properties
