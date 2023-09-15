#!/bin/bash

mkdir -p logs || exit 1

if [ "$#" -ne 1 ]; then
    echo "Incorrect number of arguments."
    echo "usage: docker-run.sh <# PARTICIPANTS>"
    exit 1
fi

STREAMLET_PARTICIPANTS=$1

for (( i=0; i<STREAMLET_PARTICIPANTS; i++ )); do
    ./docker-run-local.sh $i $STREAMLET_PARTICIPANTS $((8080+i+1)) localhost:9092 > logs/node$i.log &
    echo "Started node $i"
done