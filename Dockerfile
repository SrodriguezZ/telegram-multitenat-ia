FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace/app

COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn

RUN chmod +x ./mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src

RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
