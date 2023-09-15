#!/bin/bash

STREAMLET_PARTICIPANTS=10

docker container prune -f

docker run --init \
-e STREAMLET_PARTICIPANTS=$STREAMLET_PARTICIPANTS \
-p 8080:8080 \
-p 9092:9092 \
alexandergillon/projects:broadcast 