@echo on
cd /d %~dp0
set BatchPath=%~dp0

%BatchPath%tools\sed -i~ -e "s/\(version[[:space:]]*=[[:space:]]'[0-9][.][0-9][.][0-9][.]\)[0-9]*\(.*\)'/\1%1\2'/" %BatchPath%build.gradle
%BatchPath%tools\grep -E "version[[:space:]]*=[[:space:]]'[0-9][.][0-9][.][0-9][.][0-9]*.*'" build.gradle | %BatchPath%tools\sed -e "s/version[[:space:]]*=[[:space:]]'\([0-9][.][0-9][.][0-9][.][0-9]*.*\)'/\1/"
