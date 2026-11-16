@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-22
"%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd" -f "%~dp0backend\pom.xml" %*
