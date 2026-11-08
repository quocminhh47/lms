# syntax=docker/dockerfile:1

# --- Build stage ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache bash

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/

RUN ./mvnw -q -DskipTests package \
    && cp target/lms-*.jar /workspace/app.jar

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S -g 10001 lms \
    && adduser -S -u 10001 -G lms lms

COPY --from=build /workspace/app.jar /app/app.jar
RUN chown -R lms:lms /app

USER 10001:10001

# Render injects PORT at runtime; Spring maps server.port=${PORT:8080}
EXPOSE 8080

# Baseline JVM flags for ~512 MB containers (heap capped below container RAM for native/metaspace/threads).
# Render may set JAVA_OPTS for extra flags; both are passed to the java command (do not replace JVM_HEAP_OPTS).
ENV JVM_HEAP_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=25.0 -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JVM_HEAP_OPTS $JAVA_OPTS -jar /app/app.jar"]
