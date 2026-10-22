FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/target/order-service-1.0.0.jar app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
