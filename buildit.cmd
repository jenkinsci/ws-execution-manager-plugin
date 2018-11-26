@echo on
cd /d %~dp0
set BatchPath=%~dp0

rem set JAVA_HOME=c:\program files\java\jdk1.8.0_131
cmd /c .\gradlew jpi

dir build\libs
