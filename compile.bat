@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   Compiling Suffix Index Builder (100%% Java SE 21)
echo ========================================================

if not exist bin mkdir bin

dir /s /b src\main\java\*.java src\test\java\*.java > sources.txt

javac -d bin -encoding UTF-8 @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%

del sources.txt

if %COMPILE_STATUS% EQU 0 (
    echo [OK] Compilation succeeded. Class files generated in bin/
) else (
    echo [FAIL] Compilation failed with error code %COMPILE_STATUS%
    exit /b %COMPILE_STATUS%
)
