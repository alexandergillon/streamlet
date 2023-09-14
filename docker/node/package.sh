#!/bin/bash

mkdir -p out || exit 1
mkdir -p out/keystore || exit 1

STREAMLET_BROADCAST_VERSION="0.0.1-SNAPSHOT"
STREAMLET_BROADCAST_JAR_NAME="node-$STREAMLET_BROADCAST_VERSION.jar"

cd ../../node
./mvnw clean package spring-boot:repackage -Dmaven.test.skip=true || exit 1
cd ..

cp node/target/$STREAMLET_BROADCAST_JAR_NAME docker/node/out/node.jar || exit 1
cp node/src/main/resources/secrets-docker.properties docker/node/out/secrets.properties || exit 1
cp -r node/src/main/resources/keystore/public docker/node/out/keystore/public || exit 1
cp -r node/src/main/resources/keystore/private docker/node/out/keystore/private || exit 1

cd docker/node
docker build -t alexandergillon/projects:node . || exit 1
docker image prune -f || exit 1