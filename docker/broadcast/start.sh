#!/bin/bash

./kafka/bin/kafka-server-start.sh kafka/config/kraft/server.properties &
sleep 5
java -jar broadcast/broadcast.jar &

wait -n
exit $?