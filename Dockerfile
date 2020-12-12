FROM openjdk:11.0.6-jdk
MAINTAINER xobotun

COPY ./rx_martian_agrobot.jar .

EXPOSE 80
ENTRYPOINT ["java", "-jar", "rx_martian_agrobot.jar"]
