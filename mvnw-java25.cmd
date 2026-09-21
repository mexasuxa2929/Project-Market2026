@echo off
setlocal

set "PROJECT_JAVA_HOME=%USERPROFILE%\.jdks\openjdk-25.0.2"

if not exist "%PROJECT_JAVA_HOME%\bin\java.exe" (
  echo Project JDK not found: "%PROJECT_JAVA_HOME%"
  echo Install or configure a compatible JDK, then update mvnw-java25.cmd.
  exit /b 1
)

set "JAVA_HOME=%PROJECT_JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "JDK_JAVA_OPTIONS=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow %JDK_JAVA_OPTIONS%"

call "%~dp0mvnw.cmd" %*
