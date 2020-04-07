FROM maven:3.6.3-jdk-8 as build
RUN mkdir -p /kobot/src
COPY src /kobot/src
COPY pom.xml /kobot
WORKDIR /kobot
RUN mvn clean package

FROM openjdk:8
RUN mkdir -p /kobot
COPY --from=build /kobot/target/kobot-1.0-SNAPSHOT-jar-with-dependencies.jar /kobot/kobot-1.0.jar
WORKDIR /kobot
ENTRYPOINT ["java", "-jar", "kobot-1.0.jar" ]

#FROM oracle/graalvm-ce:20.0.0
#RUN gu install native-image
#RUN mkdir -p /kobot
#COPY --from=build /kobot/target/kobot-1.0-SNAPSHOT-jar-with-dependencies.jar /kobot/kobot-1.0.jar
#WORKDIR /kobot




