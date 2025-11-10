\
@echo off
setlocal enabledelayedexpansion
cd /d %~dp0
call mvn -q -DskipTests package
java -jar target\uml-visualizer-0.1.0-all.jar .\example-src diagram.puml
echo Render with: plantuml diagram.puml
