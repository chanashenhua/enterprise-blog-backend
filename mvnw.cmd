@echo off
setlocal
set "BASEDIR=%~dp0"
if "%BASEDIR:~-1%"=="\" set "BASEDIR=%BASEDIR:~0,-1%"
set "WRAPPER_JAR=%BASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "JAVACMD=java"
if defined JAVA_HOME set "JAVACMD=%JAVA_HOME%\bin\java.exe"
if defined JAVA_HOME if not exist "%JAVACMD%" (
  echo JAVA_HOME does not point to a valid Java installation. 1>&2
  exit /b 1
)
"%JAVACMD%" "-Dmaven.multiModuleProjectDirectory=%BASEDIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
set "MAVEN_EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %MAVEN_EXIT_CODE%
