FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn ./.mvn
COPY mvnw .

RUN chmod +x ./mvnw && ./mvnw dependency:resolve

COPY src ./src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/payment-service-*.jar payment-service.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "payment-service.jar"]

