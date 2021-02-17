
FROM openjdk:8

ENV APP_DIR /opt/app

RUN mkdir /opt/app && \
    apt-get update && \
    apt-get --assume-yes install vim unzip

WORKDIR $APP_DIR

COPY ./webapp/target/scala-*/webapp*assembly*.jar $APP_DIR/app.jar

RUN chmod -R 755 /opt/app

ENTRYPOINT ["java", "-jar", "app.jar"]