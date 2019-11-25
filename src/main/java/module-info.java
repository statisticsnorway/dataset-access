module no.ssb.datasetaccess {
    requires io.micronaut.http;
    requires io.reactivex.rxjava2;
    requires io.micronaut.inject;
    requires io.micronaut.runtime;
    requires io.micronaut.tracing;
    requires io.micronaut.core;
    requires javax.inject;
    requires slf4j.api;
    requires logstash.logback.encoder;
    requires reactive.pg.client;
    requires java.sql; // required by micronaut-metrics
    requires jdk.unsupported; // required by netty to allow reliable low-level API access to direct-buffers
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    opens no.ssb.datasetaccess;

    exports no.ssb.datasetaccess;
}