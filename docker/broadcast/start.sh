#!/bin/bash

./kafka/bin/kafka-server-start.sh kafka/config/kraft/server.properties --override advertised.listeners=$KAFKA_ADVERTISED_LISTENERS &
sleep 5
java -jar broadcast/broadcast.jar &

wait -n
exit $?