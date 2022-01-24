# builder - first stage to build the application
FROM maven:3.5.4-jdk-11-slim AS build-env
ADD ./pom.xml pom.xml
RUN mvn dependency:go-offline
ADD ./src src/
RUN mvn clean package

# runtime - build final runtime image
FROM openjdk:11-jre-slim

#USER 1001

# add the application's jar to the container
COPY --from=build-env target/spring-todo-app-2.0-SNAPSHOT.jar app.jar
COPY appinsights/* .

# run application
EXPOSE 8080

ENV DB_PORT=5432 \
    DB_USER="myadmin" \
    DB_PASSWORD="@dmin12345678" \
    DB_SERVER="pgflexakubicharm.postgres.database.azure.com" \
    DB_NAME="todoapp"
ENTRYPOINT ["java", "-javaagent:/applicationinsights-agent-3.0.3.jar", "-jar","/app.jar"]
