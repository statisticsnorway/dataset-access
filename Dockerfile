FROM alpine:latest as build

RUN apk --no-cache add curl tar gzip binutils

#
# Install JDK
#
RUN curl https://cdn.azul.com/zulu/bin/zulu13.28.11-ca-jdk13.0.1-linux_musl_x64.tar.gz -o /jdk.tar.gz
RUN mkdir -p /jdk
RUN tar xzf /jdk.tar.gz --strip-components=1 -C /jdk
ENV PATH=/jdk/bin:$PATH
ENV JAVA_HOME=/jdk

#
# Build stripped JVM
#
RUN ["jlink", "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2", "--module-path", "/jdk/jmods", "--output", "/linked",\
 "--add-modules", "java.base,java.management,jdk.unsupported,java.sql,jdk.zipfs,jdk.naming.dns,java.desktop,java.net.http"]

#
# Build Application image
#
FROM alpine:latest

#
# Resources from build image
#
COPY --from=build /linked /jdk/
COPY target/libs /app/lib/
COPY target/dapla-auth-dataset-service*.jar /app/lib/
COPY target/classes/logback.xml /app/conf/
COPY target/classes/logback-bip.xml /app/conf/
COPY target/classes/application.yaml /app/conf/

ENV PATH=/jdk/bin:$PATH

WORKDIR /app

EXPOSE 8080
EXPOSE 7070

CMD ["java", "-p", "/app/lib", "-m", "no.ssb.datasetaccess/no.ssb.datasetaccess.Application"]
