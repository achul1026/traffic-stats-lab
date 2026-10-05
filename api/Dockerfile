# 멀티 스테이지 빌드: 빌드 이미지에서 jar 를 만들고, 실행 이미지에는 JRE 만 둔다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon -q dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon -q bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Xmx512m"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
