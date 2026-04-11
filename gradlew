#!/bin/sh
# Gradle wrapper script

APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
APP_NAME="Gradle"
APP_BASE_NAME="$(basename "$0")"

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
