FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw package -DskipTests -q

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
VOLUME ["/app/logs"]
ENTRYPOINT ["java", "-jar", "app.jar"]
