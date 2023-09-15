# Java 17 JDK base image
FROM eclipse-temurin:17

ARG KAFKA_CLUSTER_ID=gcCgGGBNSOCN_tdgBHqrTQ
COPY kafka kafka
RUN kafka/bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c kafka/config/kraft/server.properties

ENTRYPOINT ./kafka/bin/kafka-server-start.sh kafka/config/kraft/server.properties --override advertised.listeners=$KAFKA_ADVERTISED_LISTENERS