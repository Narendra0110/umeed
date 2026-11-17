@echo off
rem Builds the Umeed app with JDK 17 and starts it on the Android phone connected by USB.
rem Usage (from apps\mobile):
rem   run-android          build and run
rem   run-android clean    regenerate the android folder first (after changing app.json or google-services.json)
rem   run-android sha1     show the SHA-1 of the debug signing key (to add in the Firebase console)
setlocal

set "JAVA_HOME="
for /d %%D in ("C:\Program Files\Microsoft\jdk-17*") do set "JAVA_HOME=%%D"
if not defined JAVA_HOME (
    echo JDK 17 was not found in C:\Program Files\Microsoft.
    echo Install it with:  winget install Microsoft.OpenJDK.17
    exit /b 1
)
if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"
cd /d "%~dp0"

if /i "%~1"=="sha1" (
    if not exist "android\app\debug.keystore" (
        echo The android folder does not exist yet. Run:  run-android clean
        exit /b 1
    )
    keytool -list -v -keystore android\app\debug.keystore -alias androiddebugkey -storepass android -keypass android | findstr /c:"SHA1"
    exit /b 0
)

if /i "%~1"=="clean" (
    call npx expo prebuild --clean --platform android
    if errorlevel 1 exit /b 1
)

rem Let the phone reach the PC's backend at localhost:8080 over the USB cable.
adb reverse tcp:8080 tcp:8080
if errorlevel 1 (
    echo No Android device found. Connect the phone by USB with USB debugging on, then check with:  adb devices
    exit /b 1
)

call npx expo run:android
