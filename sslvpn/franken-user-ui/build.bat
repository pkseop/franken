@echo off
setlocal enabledelayedexpansion

rem copy cab files to franken-user-ui
echo copying cab files to franken-user-ui dir
copy ..\..\..\buildtools\sslplus.cab "themes\common\sslplus.cab"
copy ..\..\..\buildtools\SSLplusDNSChecker.cab "themes\common\SSLplusDNSChecker.cab"

rem update cab file version to index.html
echo updating SSLPlusInstaller.inf
echo "pushd to frodo-activex"
rem move to frodo-activex dir
pushd ..\..\..\frodo-activex
for /D %%D in (..\franken-user-ui-build\sslvpn\franken-user-ui\themes\default*) DO (
	if EXIST %%D\index.html (
		echo updating %%D\index.html
		..\buildtools\buildman update_rc_version "." %%D\index.html 1.0.0 utf-8
	)
)
echo "popd from frodo-activex"
popd


echo "Cleaning existing WEB-INF"
rd src\main\resources\WEB-INF /S /Q

mkdir src\main\resources\WEB-INF

echo "Copy common to WEB-INF"
xcopy themes\common\*.* src\main\resources\WEB-INF /I /Y /S

echo "Copy %2 to WEB-INF"
xcopy themes\%2\*.* src\main\resources\WEB-INF /I /Y /S

rem read version from pom.xml
set /A count=0
set "xmlFile=pom.xml"

for /f "tokens=1,2,3 delims=<>" %%n in ('findstr /i /c:"<version>" "%xmlFile%"') do call :work %%p 

goto :endLoop

:work
	if  %count% == 2	(goto :eof)
	set /A count+=1
	set "version=%1"
	goto :eof
	
:endLoop
	if not "%2" == "default" (set "version=%version%.%2")
	echo "version is %version%"

rem build jar by mvn
call mvn versions:revert
call mvn versions:set -DnewVersion=%version%
call mvn %1 versions:revert -DnewVersion=%version%