@echo off
rem Starts the Pixel_7 Android emulator without Android Studio.
rem The first start after creating the device is a cold boot and can take 10 minutes or more.
rem Later starts use the saved quick-boot snapshot and are much faster.
rem Usage (from apps\mobile):
rem   start-emulator          normal start (quick boot)
rem   start-emulator cold     full restart, ignoring the saved snapshot
setlocal

if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if not defined ANDROID_AVD_HOME set "ANDROID_AVD_HOME=H:\android-avd"

if /i "%~1"=="cold" (
    start "Android emulator" "%ANDROID_HOME%\emulator\emulator.exe" -avd Pixel_7 -no-snapshot-load
) else (
    start "Android emulator" "%ANDROID_HOME%\emulator\emulator.exe" -avd Pixel_7
)

echo Emulator starting. Wait until the Android home screen appears before running run-android.
