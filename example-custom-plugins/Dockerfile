FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/custom-plugins-example-1.0.0-uberjar.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
