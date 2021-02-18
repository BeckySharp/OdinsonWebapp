FROM openjdk:8

ENV APP_DIR /opt/odinson

RUN apt-get --assume-yes update && \
    mkdir /opt/app

WORKDIR $APP_DIR

COPY ./webapp/target/universal/webapp*.zip $APP_DIR/app.zip

RUN unzip -q $APP_DIR/app.zip && \
    export APP=$(ls -d */ | grep webapp) && \
    mv $APP app

ENTRYPOINT $APP_DIR/app/bin/webapp
