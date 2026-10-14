FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

ARG JAR_FILE=services/inventory-service/build/libs/inventory-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

RUN chown appuser:appgroup app.jar
USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8085

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
