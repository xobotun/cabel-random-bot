FROM openjdk:11.0.6-jdk
MAINTAINER xobotun

COPY ./cabel-random-bot.jar .

EXPOSE 80
ENTRYPOINT ["java", "-jar", "cabel-random-bot.jar"]
