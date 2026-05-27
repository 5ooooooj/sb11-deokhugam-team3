# 빌드 스테이지
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true
COPY src ./src
RUN gradle clean build -x test --no-daemon

# 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
RUN mkdir -p /app/logs && chown -R app:app /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER app
# 최대 힙 512m, 초기 힙 256m 제한
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]