#!/bin/sh
git diff-index --quiet HEAD -- || { echo "Uncommmitted changes detected- please run this script from a clean working directory."; exit; }
mvn clean package
mkdir -p target/webserver/library
cp library.properties target/webserver
mv target/webserver.jar target/webserver/library
mv target/dependency/* target/webserver/library
cd target
zip -r webserver.zip webserver
