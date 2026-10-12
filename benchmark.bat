@echo off
echo Running Suffix Index Builder Performance Benchmarks...
if not exist bin\com\suffixindex\test\PerformanceBenchmark.class (
    echo Compiling first...
    call compile.bat
)
java -Xmx2g -cp bin com.suffixindex.test.PerformanceBenchmark
