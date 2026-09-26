FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -pl catalog-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 flowcart
COPY --from=build /workspace/catalog-service/target/catalog-service-0.1.0-SNAPSHOT.jar /app/app.jar
USER flowcart
EXPOSE 8081
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
