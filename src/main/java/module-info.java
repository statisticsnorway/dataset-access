module no.ssb.datasetaccess {
    requires io.micronaut.http;
    requires io.reactivex.rxjava2;
    requires io.micronaut.inject;
    requires io.micronaut.runtime;
    requires javax.inject;
    requires slf4j.api;
    requires logstash.logback.encoder;
    requires reactive.pg.client;
    requires java.sql; // required by micronaut-metrics

    exports no.ssb.datasetaccess;
}