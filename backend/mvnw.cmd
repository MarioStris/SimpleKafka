@REM Maven Wrapper script for Windows
@REM Downloads Maven if needed and runs it

@echo off
setlocal

set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6
set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven 3.9.6...
    mkdir "%MAVEN_HOME%" 2>nul
    set TMPFILE=%TEMP%\maven-download.zip
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%TEMP%\maven-download.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\maven-download.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
    del "%TEMP%\maven-download.zip" 2>nul
    echo Maven downloaded.
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
