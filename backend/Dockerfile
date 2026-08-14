FROM maven:3.9.9-eclipse-temurin-21 AS build-base

WORKDIR /app

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

FROM build-base AS test

COPY src ./src
RUN set +e; \
    mvn -B clean test; \
    TEST_EXIT_CODE=$?; \
    mkdir -p /test-results/surefire-reports; \
    cp -r target/surefire-reports/. /test-results/surefire-reports/ 2>/dev/null || true; \
    printf '%s' "$TEST_EXIT_CODE" > /test-results/exit-code; \
    exit 0

FROM build-base AS package

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY --from=package /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
