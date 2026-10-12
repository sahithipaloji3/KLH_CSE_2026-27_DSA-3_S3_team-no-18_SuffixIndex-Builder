@echo off
echo ===================================================
echo   Launching Suffix Index Builder Desktop GUI...
echo ===================================================
if not exist bin\com\suffixindex\Main.class (
    echo Binaries not found. Running compile.bat first...
    call compile.bat
)
java -cp bin com.suffixindex.Main
