FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY . .

RUN mvn -q -Dmaven.test.skip=true package -pl payment-service -am

FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

COPY --from=builder /build/payment-service/target/*.jar /app/app.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
