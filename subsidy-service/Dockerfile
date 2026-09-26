FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY subsidy-service/target/subsidy-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
