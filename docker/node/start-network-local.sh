#!/bin/bash

mkdir -p logs || exit 1

if [ "$#" -ne 1 ]; then
    echo "Incorrect number of arguments."
    echo "usage: docker-run.sh <# PARTICIPANTS>"
    exit 1
fi

STREAMLET_PARTICIPANTS=$1

if ! [[ $STREAMLET_PARTICIPANTS =~ ^[0-9]+$ ]]; then
    echo "Supplied number of participants is not a positive integer."
    exit 1
fi

rm logs/* || exit 1

for (( i=0; i<STREAMLET_PARTICIPANTS; i++ )); do
    ./docker-run.sh $i $STREAMLET_PARTICIPANTS $((8080+i+1)) 172.17.0.1:9092 > logs/node$i.log &
    echo "Started node $i"
done