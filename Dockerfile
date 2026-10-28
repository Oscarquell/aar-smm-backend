FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package \
    && install -D target/portfolio-backend-0.0.1-SNAPSHOT.jar /workspace/app.jar

FROM eclipse-temurin:25-jre-noble

WORKDIR /app
RUN useradd --system --uid 10001 --create-home app
COPY --from=build --chown=10001:10001 /workspace/app.jar /app/app.jar
USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
