@echo off
rem Compiles the project and starts the server (Windows).
cd /d "%~dp0"

if not exist lib\*.jar (
  echo MySQL driver missing: put mysql-connector-j-*.jar into the lib folder ^(see README.md^).
  exit /b 1
)

if not exist out mkdir out
javac -encoding UTF-8 -d out src\driveease\*.java || exit /b 1
java -cp "out;lib\*" driveease.Main
