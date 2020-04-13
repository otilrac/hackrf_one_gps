@echo off
set JAVA_HOME=build/jre
set PATH=%PATH%;%JAVA_HOME%/bin
where java
java -jar build/bin/hackrf_one_gps-1.0.0-SNAPSHOT.jar
pause