FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY . .

RUN mvn -q -Dmaven.test.skip=true package -pl warehouse-service -am

FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

COPY --from=builder /build/warehouse-service/target/*.jar /app/app.jar

EXPOSE 8084

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -Xmx512m"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
