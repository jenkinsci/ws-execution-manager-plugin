@echo on
cd /d %~dp0
set BatchPath=%~dp0

if "%1"=="" GOTO BUILDIT
set buildEnv="-PenvironmentName=%1"

:BUILDIT
rem set JAVA_HOME=c:\program files\java\jdk1.8.0_131
cmd /c .\gradlew %buildEnv% jpi
