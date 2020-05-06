FROM ysihaoy/scala-play:2.12.2-2.6.0-sbt-0.13.15

# Install Odinson
RUN apk --no-cache add git
RUN git clone https://github.com/lum-ai/odinson /tmp/odinson
RUN cd /tmp/odinson && \
  sbt publishLocal


# caching dependencies
COPY ["build.sbt", "/tmp/build/"]
COPY ["project/plugins.sbt", "project/build.properties", "/tmp/build/project/"]
RUN cd /tmp/build && \
  sbt compile && \
  sbt test:compile && \
 rm -rf /tmp/build

# copy code
COPY . /root/webapp
WORKDIR /root/webapp

EXPOSE 9000
CMD sbt webapp/run