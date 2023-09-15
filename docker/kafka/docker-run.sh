#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Incorrect number of arguments."
    echo "usage: docker-run.sh <KAFKA_ADVERTISED_LISTENERS>"
    exit 1
fi

KAFKA_ADVERTISED_LISTENERS=$1

docker container prune -f

docker run --init \
-e KAFKA_ADVERTISED_LISTENERS=$KAFKA_ADVERTISED_LISTENERS \
-p 9092:9092 \
alexandergillon/projects:kafka 