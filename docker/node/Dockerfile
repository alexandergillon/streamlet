# Java 17 JDK base image
FROM eclipse-temurin:17

COPY out/secrets.properties node/secrets.properties
COPY out/keystore node/keystore
COPY out/node.jar node/node.jar

ENTRYPOINT [ "java", "-Dspring.config.additional-location=node/secrets.properties", "-jar", "node/node.jar" ]