#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Incorrect number of arguments."
    echo "usage: docker-run.sh <# PARTICIPANTS> <KAFKA_ADVERTISED_LISTENERS>"
    exit 1
fi

STREAMLET_PARTICIPANTS=$1
KAFKA_ADVERTISED_LISTENERS=$2

if ! [[ $STREAMLET_PARTICIPANTS =~ ^[0-9]+$ ]]; then
    echo "Supplied number of participants is not a positive integer."
    exit 1
fi

docker container prune -f

docker run --init \
-e STREAMLET_PARTICIPANTS=$STREAMLET_PARTICIPANTS \
-e KAFKA_ADVERTISED_LISTENERS=$KAFKA_ADVERTISED_LISTENERS \
-p 8080:8080 \
-p 9092:9092 \
alexandergillon/projects:broadcast 