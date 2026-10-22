@echo off
setlocal
cd /d "%~dp0"
echo ========================================
echo        DriveEase - No Maven Launcher
echo ========================================
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found. Install a JDK 17 or later and reopen this window.
  pause
  exit /b 1
)
where javac >nul 2>nul
if errorlevel 1 (
  echo ERROR: javac was not found. You need a JDK 17 or later, not just a JRE.
  pause
  exit /b 1
)
dir /b "lib\mysql-connector-j-*.jar" >nul 2>nul
if errorlevel 1 (
  echo ERROR: MySQL Connector/J is missing.
  echo Download MySQL Connector/J from the official MySQL website,
  echo extract it, and copy the mysql-connector-j-8.4.0.jar into:
  echo   %CD%\lib\
  pause
  exit /b 1
)
if not exist "config.properties" (
  copy "config.properties.example" "config.properties" >nul
  echo Created config.properties from the example.
  echo IMPORTANT: Set db.password to your MySQL password, save, then run again.
  notepad "config.properties"
  pause
  exit /b 0
)
if not exist "out" mkdir "out"
echo Compiling DriveEase Java source files...
javac --add-modules jdk.httpserver -cp "lib\*" -d out src\main\java\driveease\*.java
if errorlevel 1 (
  echo Compilation failed. Check the error messages above.
  pause
  exit /b 1
)
echo Starting DriveEase...
echo Open http://localhost:5000 in your browser. Keep this window open.
java --add-modules jdk.httpserver -cp "out;lib\*" driveease.Main
pause
