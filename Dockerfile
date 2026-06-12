FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG JAR_FILE=build/libs/app.jar
ARG OTEL_JAVA_AGENT_VERSION=2.18.1

RUN mkdir -p /otel \
    && curl -fsSL \
        "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar" \
        -o /otel/opentelemetry-javaagent.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
