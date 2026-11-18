@echo off
rem Runs the backend build and tests, the mobile app's type check and tests, and the admin web app's build.
rem Only if all of them pass, commits everything and pushes.
rem Usage (from the project root):  ship "commit message"
setlocal

if "%~1"=="" (
    echo Usage: ship "commit message"
    exit /b 1
)

if "%UMEED_DB_PASSWORD%"=="" (
    echo UMEED_DB_PASSWORD is not set.
    echo Set it once with:  setx UMEED_DB_PASSWORD yourpassword
    echo Then open a new command window and run ship again.
    exit /b 1
)

pushd "%~dp0backend" || exit /b 1
call mvnw.cmd test
set BUILD_RESULT=%ERRORLEVEL%
popd

if not "%BUILD_RESULT%"=="0" goto build_failed

echo.
echo ===== Mobile app: type check and tests =====
pushd "%~dp0apps\mobile" || exit /b 1
call npx tsc --noEmit
if errorlevel 1 goto mobile_failed
call npx jest --ci
if errorlevel 1 goto mobile_failed
popd

echo.
echo ===== Admin web app: type check and build =====
pushd "%~dp0apps\admin" || exit /b 1
set NEXT_TELEMETRY_DISABLED=1
call npm run build
if errorlevel 1 goto admin_failed
popd

pushd "%~dp0" || exit /b 1
git add -A
git diff --cached --quiet
if "%ERRORLEVEL%"=="0" goto nothing_to_commit

git commit -m "%~1"
if not "%ERRORLEVEL%"=="0" goto commit_failed
goto push

:nothing_to_commit
echo.
echo Tests passed. There was nothing new to commit.

:push
git push
set PUSH_RESULT=%ERRORLEVEL%
popd
if not "%PUSH_RESULT%"=="0" goto push_failed

echo.
echo ===== SHIPPED: tests passed, committed and pushed. =====
exit /b 0

:build_failed
echo.
echo ===== BUILD FAILED. Nothing was committed. =====
exit /b 1

:mobile_failed
popd
echo.
echo ===== MOBILE CHECK FAILED (type check or tests). Nothing was committed. =====
exit /b 1

:admin_failed
popd
echo.
echo ===== ADMIN WEB APP BUILD FAILED. Nothing was committed. =====
exit /b 1

:commit_failed
popd
echo.
echo ===== Tests passed, but the COMMIT FAILED. Nothing was pushed. =====
exit /b 1

:push_failed
echo.
echo ===== Tests passed and committed, but the PUSH FAILED. Run "git push" again. =====
exit /b 1
