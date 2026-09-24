#!/usr/bin/env bash
# Compiles the project and starts the server (Linux / macOS).
set -e
cd "$(dirname "$0")"

if ! ls lib/*.jar >/dev/null 2>&1; then
  echo "MySQL driver missing: put mysql-connector-j-*.jar into the lib/ folder (see README.md)."
  exit 1
fi

mkdir -p out
javac -encoding UTF-8 -d out src/driveease/*.java
java -cp "out:lib/*" driveease.Main
