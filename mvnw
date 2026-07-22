#!/bin/sh
BASEDIR=$(dirname "$0")
exec java "-Dmaven.multiModuleProjectDirectory=$BASEDIR" -classpath "$BASEDIR/.mvn/wrapper/maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain "$@"
