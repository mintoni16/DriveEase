#!/bin/bash
mkdir -p out
JAR=$(ls lib/mysql-connector-j-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  echo "Put MySQL Connector/J jar inside lib/"
  exit 1
fi
javac -cp "$JAR" -d out src/*.java src/model/*.java src/dao/*.java src/handler/*.java src/util/*.java || exit 1
java -cp "out:$JAR" Main
