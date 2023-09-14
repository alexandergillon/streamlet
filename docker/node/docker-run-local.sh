#!/bin/bash

# This script runs a node container with Docker network in 'host' mode. Spring Boot server runs on STREAMLET_NODE_PORT.

if [ "$#" -ne 3 ]; then
    echo "Incorrect number of arguments."
    echo "usage: docker-run-local.sh <NODE-ID> <PORT> <KAFKA-BOOTSTRAP-SERVERS>"
    exit 1
fi

STREAMLET_PARTICIPANTS=5
STREAMLET_EPOCH_DURATION=2000

STREAMLET_NODE_ID=$1
STREAMLET_NODE_PORT=$2
STREAMLET_KAFKA_BOOTSTRAP_SERVERS=$3

if ! [[ $STREAMLET_NODE_ID =~ ^[0-9]+$ ]]; then
    echo "Supplied node ID is not a positive integer."
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

docker run --init \
-e STREAMLET_PARTICIPANTS=$STREAMLET_PARTICIPANTS \
-e STREAMLET_NODE_ID=$STREAMLET_NODE_ID \
-e STREAMLET_EPOCH_DURATION=$STREAMLET_EPOCH_DURATION \
-e STREAMLET_KAFKA_BOOTSTRAP_SERVERS=$STREAMLET_KAFKA_BOOTSTRAP_SERVERS \
-e SERVER_PORT=$STREAMLET_NODE_PORT \
--network="host" \
alexandergillon/projects:node 