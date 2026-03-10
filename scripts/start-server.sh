#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-8080}"
JAR_PATH="target/microspringboot-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_PATH" ]; then
  echo "Jar not found at $JAR_PATH. Run mvn clean package first."
  exit 1
fi

nohup java -jar "$JAR_PATH" "$PORT" > server.log 2>&1 &
echo "MicroSpringBoot started on port $PORT"
echo "PID: $!"