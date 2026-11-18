@echo off
rem Builds the runnable backend file for the server (H02): backend\target\backend.jar
rem Runs all backend tests first, like ship. Usage (from the project root):  build-backend
setlocal

if "%UMEED_DB_PASSWORD%"=="" (
    echo UMEED_DB_PASSWORD is not set. The tests need the local database.
    exit /b 1
)

pushd "%~dp0backend" || exit /b 1
call mvnw.cmd clean package
set RESULT=%ERRORLEVEL%
popd

if not "%RESULT%"=="0" (
    echo.
    echo ===== BUILD FAILED. No backend.jar was made. =====
    exit /b 1
)

echo.
echo ===== Built: backend\target\backend.jar =====
echo Copy it to the server as /opt/umeed/backend.jar (steps in H03).
exit /b 0
