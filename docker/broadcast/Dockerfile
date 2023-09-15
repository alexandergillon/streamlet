# Java 17 JDK base image
FROM eclipse-temurin:17

COPY out/broadcast.jar broadcast/broadcast.jar

ENTRYPOINT [ "java", "-jar", "broadcast/broadcast.jar" ]