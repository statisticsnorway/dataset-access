FROM openjdk:11.0.5-slim

COPY target/dependency /app/lib/
COPY target/dataset-access-*.jar /app/lib/

WORKDIR /app

EXPOSE 8080

CMD ["java", "-Dcom.sun.management.jmxremote", "-p", "/app/lib", "-m", "no.ssb.datasetaccess/no.ssb.datasetaccess.Application"]
