# 빌드 스테이지
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle/ ./gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew clean build -x test --no-daemon

# 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update && DEBIAN_FRONTEND=noninteractive TZ=Asia/Seoul \
    apt-get install -y --no-install-recommends tzdata && \
    ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo "$TZ" > /etc/timezone && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -r app && useradd -r -g app app
RUN mkdir -p /app/logs && chown -R app:app /app

COPY --from=builder /app/build/libs/*.jar app.jar
USER app
# 최대 힙 512m, 초기 힙 256m 제한
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", \
  "-jar", "app.jar"]