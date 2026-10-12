@echo off
echo Running Suffix Index Builder Test Suite...
if not exist bin\com\suffixindex\test\SuffixIndexTestSuite.class (
    echo Compiling first...
    call compile.bat
)
java -cp bin com.suffixindex.test.SuffixIndexTestSuite
