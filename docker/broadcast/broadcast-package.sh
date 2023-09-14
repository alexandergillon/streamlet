#!/bin/bash

# Create out/ directory if it does not exist
if [ ! -d "out" ]
  then 
    if [ -f "out" ]
      then
        echo File named \'out\' exists: remove so that \'out\' directory can be created.
        exit 1
      else
        mkdir out || exit 1
    fi
fi 

STREAMLET_BROADCAST_VERSION="0.0.1-SNAPSHOT"
STREAMLET_BROADCAST_JAR_NAME="broadcast-$STREAMLET_BROADCAST_VERSION.jar"

cd ../../broadcast
./mvnw clean package spring-boot:repackage -Dmaven.test.skip=true || exit 1
cd ..

cp broadcast/target/$STREAMLET_BROADCAST_JAR_NAME docker/broadcast/out/broadcast.jar || exit 1

cd docker/broadcast
docker build -t alexandergillon/projects:broadcast . || exit 1
docker image prune -f || exit 1