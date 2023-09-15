#!/bin/bash

# This script runs a node container with Docker network in 'bridge' mode. Spring Boot server runs on 8080, which is mapped to STREAMLET_NODE_PORT via Docker.

if [ "$#" -ne 4 ]; then
    echo "Incorrect number of arguments."
    echo "usage: docker-run.sh <NODE-ID> <# PARTICIPANTS> <PORT> <KAFKA-BOOTSTRAP-SERVERS>"
    exit 1
fi

STREAMLET_EPOCH_DURATION=2000

STREAMLET_NODE_ID=$1
STREAMLET_PARTICIPANTS=$2
STREAMLET_NODE_PORT=$3
STREAMLET_KAFKA_BOOTSTRAP_SERVERS=$4

if ! [[ $STREAMLET_NODE_ID =~ ^[0-9]+$ ]]; then
    echo "Supplied node ID is not a positive integer."
    exit 1
fi

if ! [[ $STREAMLET_PARTICIPANTS =~ ^[0-9]+$ ]]; then
    echo "Supplied number of participants is not a positive integer."
    exit 1
fi

if (( $STREAMLET_NODE_ID >= $STREAMLET_PARTICIPANTS )); then
    echo "Supplied node ID is out of range (> $STREAMLET_PARTICIPANTS)."
    exit 1
fi

if ! [[ $STREAMLET_NODE_PORT =~ ^[0-9]+$ ]]; then
    echo "Supplied port is not a positive integer."
    exit 1
fi

if (( $STREAMLET_NODE_PORT <= 1024 )); then
    echo "Supplied port is in reserved port range (0-1023)."
    exit 1
fi

if (( $STREAMLET_NODE_PORT > 65535 )); then
    echo "Supplied port is out of range (> 65535)."
    exit 1
fi

docker container prune -f

docker run --init \
-e STREAMLET_PARTICIPANTS=$STREAMLET_PARTICIPANTS \
-e STREAMLET_NODE_ID=$STREAMLET_NODE_ID \
-e STREAMLET_EPOCH_DURATION=$STREAMLET_EPOCH_DURATION \
-e STREAMLET_KAFKA_BOOTSTRAP_SERVERS=$STREAMLET_KAFKA_BOOTSTRAP_SERVERS \
-p $STREAMLET_NODE_PORT:8080 \
alexandergillon/projects:node 