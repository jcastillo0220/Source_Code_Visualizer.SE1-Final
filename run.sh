#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"
./mvnw -q -DskipTests package || mvn -q -DskipTests package
java -jar target/uml-visualizer-0.1.0-all.jar ./example-src diagram.puml
echo "Render with: plantuml diagram.puml"
