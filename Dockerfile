# Estágio de Build (Usando Maven com JDK 21)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio de Execução (Usando JRE 21 Alpine para ficar leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SERVER_PORT=8084
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]