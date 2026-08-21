#!/bin/bash
# 编译并启动平台 / compile and launch the platform
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -Dfile.encoding=UTF-8 -cp out robot.Main "$@"
