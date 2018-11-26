@echo on
cd /d %~dp0
set BatchPath=%~dp0

mkdir %1

copy build\libs\*.hpi %1
