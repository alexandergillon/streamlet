# Java 17 JDK base image
FROM eclipse-temurin:17

ARG KAFKA_CLUSTER_ID=gcCgGGBNSOCN_tdgBHqrTQ
COPY kafka kafka
RUN kafka/bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c kafka/config/kraft/server.properties

COPY out/broadcast.jar broadcast/broadcast.jar

COPY broadcast-run.sh broadcast-run.sh

ENTRYPOINT [ "./broadcast-run.sh" ]