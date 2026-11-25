@echo off
rem Build every Minecraft version and collect the jars in dist\
setlocal
cd /d "%~dp0"
if exist dist rmdir /s /q dist
mkdir dist
rem Only collect jars of the current version (old builds stay in build\libs)
for /f "tokens=2 delims==" %%v in ('findstr /b "version=" version.properties') do set "VER=%%v"

echo === 1.8.9 (Forge) ===
pushd legacy
call gradlew.bat build
if errorlevel 1 (
    popd
    echo 1.8.9 build failed
    exit /b 1
)
popd
rem "+" in the file name means "join files" to copy, so use xcopy
for %%f in (legacy\build\libs\DangoMusic-%VER%+1.8.9.jar) do xcopy /y /q "%%f" dist\ >nul

rem Both independent ports use the same release version.
for /f "tokens=2 delims==" %%v in ('findstr /b "portVersion=" legacy\forge112\gradle.properties') do set "PORT_VER=%%v"
echo.
echo === 1.7.10 (Forge) ===
setlocal
if not defined DANGO_JDK17 if defined JAVA_HOME_17_X64 set "DANGO_JDK17=%JAVA_HOME_17_X64%"
if not defined DANGO_JDK17 for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*" "%ProgramFiles%\Java\jdk-17*") do if exist "%%~d\bin\javac.exe" set "DANGO_JDK17=%%~d"
if defined DANGO_JDK17 set "JAVA_HOME=%DANGO_JDK17%"
pushd legacy\forge1710
call gradlew.bat build
if errorlevel 1 (
    popd
    echo 1.7.10 build failed. Set DANGO_JDK17 to JDK 17 and install the JDK 8 toolchain.
    exit /b 1
)
popd
endlocal
xcopy /y /q "legacy\forge1710\build\libs\DangoMusic-%PORT_VER%+1.7.10.jar" dist\ >nul
if errorlevel 1 exit /b 1
echo.
echo === 1.12.2 (Forge) ===
pushd legacy\forge112
call gradlew.bat build
if errorlevel 1 (
    popd
    echo 1.12.2 build failed
    exit /b 1
)
popd
xcopy /y /q "legacy\forge112\build\libs\DangoMusic-%PORT_VER%+1.12.2.jar" dist\ >nul
if errorlevel 1 exit /b 1

rem 1.20.1+ (Fabric / Forge / NeoForge) need JDK 21 (set DANGO_JDK21 to override the default path)
if not defined DANGO_JDK21 set "DANGO_JDK21=C:\Program Files\Java\jdk-21"
echo.
echo === 1.20.1+ (Fabric / Forge / NeoForge) ===
pushd modern
call gradlew.bat "-Dorg.gradle.java.home=%DANGO_JDK21%" build
if errorlevel 1 (
    popd
    echo Modern build failed
    exit /b 1
)
popd
for /d %%d in (modern\versions\*) do (
    for %%f in ("%%d\build\libs\DangoMusic-%VER%+*.jar") do xcopy /y /q "%%f" dist\ >nul
)

rem 26.x (Fabric / NeoForge) need JDK 25 (set DANGO_JDK25 to override the default path)
if not defined DANGO_JDK25 set "DANGO_JDK25=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
echo.
echo === 26.x (Fabric / NeoForge) ===
pushd modern26
call gradlew.bat "-Dorg.gradle.java.home=%DANGO_JDK25%" build
if errorlevel 1 (
    popd
    echo 26.x build failed
    exit /b 1
)
popd
for /d %%d in (modern26\versions\*) do (
    for %%f in ("%%d\build\libs\DangoMusic-%VER%+*.jar") do xcopy /y /q "%%f" dist\ >nul
)

echo.
echo Done. Jars in dist\:
dir /b dist
endlocal
