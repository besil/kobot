FROM maven:3.6.3-jdk-8 as build
RUN mkdir -p /kobot/src
COPY src /kobot/src
COPY pom.xml /kobot
WORKDIR /kobot
RUN mvn clean package

FROM openjdk:8
ARG kobotv=1.0-SNAPSHOT
RUN mkdir -p /kobot
WORKDIR /kobot
COPY --from=build /kobot/target/kobot-$kobotv-exec.jar ./
RUN echo "#!/bin/bash" > start.sh
RUN echo "java -jar kobot-$kobotv-exec.jar" >> start.sh
RUN chmod +x start.sh
ENTRYPOINT ["./start.sh" ]

