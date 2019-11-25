FROM alpine:latest as build

RUN apk --no-cache add curl tar gzip

#
# Install JDK
#
RUN curl https://cdn.azul.com/zulu/bin/zulu11.35.15-ca-jdk11.0.5-linux_musl_x64.tar.gz -o /jdk.tar.gz
RUN mkdir -p /jdk
RUN tar xzf /jdk.tar.gz --strip-components=1 -C /jdk
ENV PATH=/jdk/bin:$PATH
ENV JAVA_HOME=/jdk

#
# Build stripped JVM
#
RUN ["jlink", "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2", "--module-path", "/jdk/jmods", "--output", "/linked",\
 "--add-modules", "java.base,jdk.management.agent,jdk.unsupported,java.sql,jdk.zipfs,java.desktop,jdk.naming.dns"]

#
# Build Application image
#
FROM alpine:latest

#
# Resources from build image
#
COPY --from=build /linked /jdk/
COPY target/dependency /app/lib/
COPY target/dataset-access-*.jar /app/lib/

ENV PATH=/jdk/bin:$PATH

WORKDIR /app

EXPOSE 8080

CMD ["java", "-Dcom.sun.management.jmxremote", "-p", "/app/lib", "-m", "no.ssb.datasetaccess/no.ssb.datasetaccess.Application"]
