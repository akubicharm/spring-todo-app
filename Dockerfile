# builder - first stage to build the application
FROM maven:3-openjdk-17-slim AS build-env
ADD ./pom.xml pom.xml
RUN mvn dependency:go-offline
ADD ./src src/
RUN mvn -e clean package

# runtime - build final runtime image
FROM openjdk:17.0.2-slim

#USER 1001

# add the application's jar to the container
COPY --from=build-env target/spring-todo-app-2.0-SNAPSHOT.jar app.jar
COPY appinsights/* .

# run application
EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/applicationinsights-agent-3.0.3.jar", "-jar","/app.jar"]
