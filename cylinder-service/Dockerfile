FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY cylinder-service/target/cylinder-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
