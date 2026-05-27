FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]