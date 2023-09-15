#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Incorrect number of arguments."
    echo "usage: docker-run.sh <# PARTICIPANTS> <KAFKA-BOOTSTRAP-SERVERS>"
    exit 1
fi

STREAMLET_PARTICIPANTS=$1
STREAMLET_KAFKA_BOOTSTRAP_SERVERS=$2

if ! [[ $STREAMLET_PARTICIPANTS =~ ^[0-9]+$ ]]; then
    echo "Supplied number of participants is not a positive integer."
    exit 1
fi

docker container prune -f

docker run --init \
-e STREAMLET_PARTICIPANTS=$STREAMLET_PARTICIPANTS \
-e STREAMLET_KAFKA_BOOTSTRAP_SERVERS=$STREAMLET_KAFKA_BOOTSTRAP_SERVERS \
-p 8080:8080 \
alexandergillon/projects:broadcast 