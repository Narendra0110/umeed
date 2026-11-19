@echo off
rem Builds a standalone TEST app (APK) that reaches the backend on this PC over Wi-Fi. No USB cable and no
rem bundler are needed afterwards: copy the APK to any phone on the same Wi-Fi and install it.
rem Usage (from apps\mobile):
rem   build-test-apk                  uses this PC's Wi-Fi address, for phones (arm64)
rem   build-test-apk 192.168.1.5      uses the given address instead
rem   build-test-apk fresh            regenerates the android folder first (after changing app.json; slow)
rem Result: apps\mobile\dist\umeed-test.apk
rem Before testing: start the backend, and allow port 8080 once in Windows Firewall (see the index).
rem Test builds allow plain http:// (app.config.js); they are never for the Play Store.
setlocal

set "JAVA_HOME="
for /d %%D in ("C:\Program Files\Microsoft\jdk-17*") do set "JAVA_HOME=%%D"
if not defined JAVA_HOME (
    echo JDK 17 was not found in C:\Program Files\Microsoft.
    exit /b 1
)
if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"
cd /d "%~dp0"

set "PC_IP=%~1"
if /i "%PC_IP%"=="fresh" set "PC_IP="
if not defined PC_IP (
    for /f "usebackq delims=" %%I in (`powershell -NoProfile -Command "$c = Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1; if ($c) { $c.IPv4Address[0].IPAddress }"`) do set "PC_IP=%%I"
)
if not defined PC_IP (
    echo Could not find this PC's network address. Give it yourself, for example:  build-test-apk 192.168.1.5
    exit /b 1
)

set "UMEED_TEST_BUILD=1"
set "EXPO_PUBLIC_API_URL=http://%PC_IP%:8080"
set "EXPO_NO_GIT_STATUS=1"
echo Building a test app that talks to %EXPO_PUBLIC_API_URL%
echo.

rem Apply app.json and app.config.js (with the test-build setting) to the android folder, but only when the
rem folder is missing or not yet prepared for test builds: expo prebuild deletes and regenerates the folder,
rem which forces a full rebuild (about an hour on this PC). Use "build-test-apk fresh" after changing app.json.
set "PREPARED="
if exist android\app\src\main\AndroidManifest.xml (
    findstr /c:"usesCleartextTraffic=\"true\"" android\app\src\main\AndroidManifest.xml >nul && set "PREPARED=1"
)
if /i "%~1"=="fresh" set "PREPARED="
if /i "%~2"=="fresh" set "PREPARED="
if defined PREPARED (
    echo The android folder is already prepared for test builds; skipping expo prebuild.
) else (
    call npx expo prebuild --platform android
    if errorlevel 1 goto failed
)

pushd android
rem Full path: some shells do not run programs from the current folder by name.
rem Code shrinking (R8) is off for test builds: with it on, the app crashed at start because R8 removed the
rem WorkManager database class (androidx.work.impl.WorkDatabase). Test builds do not need to be small, and
rem their crash logs stay readable. The Play Store build (M03) needs the right keep rules instead.
call "%~dp0android\gradlew.bat" assembleRelease -PreactNativeArchitectures=arm64-v8a -Pandroid.enableMinifyInReleaseBuilds=false
set RESULT=%ERRORLEVEL%
popd
if not "%RESULT%"=="0" goto failed

if not exist dist mkdir dist
copy /y android\app\build\outputs\apk\release\app-release.apk dist\umeed-test.apk >nul
if errorlevel 1 goto failed

echo.
echo ===== Built: apps\mobile\dist\umeed-test.apk  (backend address %EXPO_PUBLIC_API_URL%) =====
echo Copy it to the phone (USB file transfer, Google Drive or WhatsApp to yourself) and open it to install.
echo The phone must be on the same Wi-Fi as this PC, and the backend must be running.
exit /b 0

:failed
echo.
echo ===== TEST APP BUILD FAILED =====
exit /b 1
