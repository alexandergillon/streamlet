#!/bin/bash

docker build -t alexandergillon/projects:kafka . || exit 1
docker image prune -f || exit 1