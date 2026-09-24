@echo off
if not exist out mkdir out
echo Compiling DriveEase...
javac -cp "lib\mysql-connector-j-9.x.x.jar" -d out src\*.java src\model\*.java src\dao\*.java src\handler\*.java src\util\*.java
if errorlevel 1 (
  echo.
  echo Compilation failed. Check the MySQL Connector/J filename in run.bat.
  pause
  exit /b 1
)
echo Starting DriveEase backend...
java -cp "out;lib\mysql-connector-j-9.x.x.jar" Main
pause
