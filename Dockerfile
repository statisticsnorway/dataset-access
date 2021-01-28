FROM eu.gcr.io/prod-bip/alpine-jdk15-buildtools:master-7744b1c6a23129ceaace641d6d76d0a742440b58 as build

#
# Build stripped JVM
#
RUN ["jlink", "--strip-java-debug-attributes", "--no-header-files", "--no-man-pages", "--compress=2", "--module-path", "/jdk/jmods", "--output", "/linked",\
 "--add-modules", "jdk.jcmd,jdk.jartool,jdk.jdi,jdk.jfr,java.management,jdk.unsupported,java.sql,jdk.zipfs,jdk.naming.dns,java.desktop,java.net.http"]

#
# Build Application image
#
FROM alpine:latest

RUN apk --no-cache add curl tar gzip nano jq

#
# Resources from build image
#
COPY --from=build /linked /jdk/
COPY --from=build /opt/jdk/bin/jar /opt/jdk/bin/jcmd /opt/jdk/bin/jdb /opt/jdk/bin/jfr /opt/jdk/bin/jinfo /opt/jdk/bin/jmap /opt/jdk/bin/jps /opt/jdk/bin/jstack /opt/jdk/bin/jstat /jdk/bin/
COPY target/libs /app/lib/
COPY target/dapla-auth-dataset-service*.jar /app/lib/
COPY target/classes/logback.xml /app/conf/
COPY target/classes/logback-bip.xml /app/conf/
COPY target/classes/application.yaml /app/conf/

ENV PATH=/jdk/bin:$PATH

WORKDIR /app

EXPOSE 10100

CMD ["java", "-p", "/app/lib", "-m", "no.ssb.useraccess/no.ssb.useraccess.UserAccessApplication"]
