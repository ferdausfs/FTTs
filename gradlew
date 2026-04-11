#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  echo "ERROR: Java not found. Set JAVA_HOME or install Java 17+." >&2
  exit 1
fi

exec "$JAVACMD" -Xmx128m -Xms64m -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
