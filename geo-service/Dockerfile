FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY . .

RUN mvn -q -Dmaven.test.skip=true package -pl geo-service -am

FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

COPY --from=builder /build/geo-service/target/*.jar /app/app.jar

EXPOSE 8095

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
