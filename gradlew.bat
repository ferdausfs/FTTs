@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
setlocal
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
