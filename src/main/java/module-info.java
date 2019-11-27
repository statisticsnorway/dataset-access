module no.ssb.datasetaccess {
    requires io.micronaut.http;
    requires io.reactivex.rxjava2;
    requires io.micronaut.inject;
    requires io.micronaut.runtime;
    requires io.micronaut.tracing;
    requires io.micronaut.core;
    requires javax.inject;
    requires org.slf4j;
    requires logstash.logback.encoder;
    requires reactive.pg.client;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires vertx.core;

    /*
     * Not so well documented requirements are declared here to force fail-fast with proper error message if
     * missing from jvm.
     */
    requires java.sql; // required by micronaut-metrics
    //requires java.desktop; // required by snakeyaml for bean introspection
    requires jdk.zipfs; // required by micronaut for classpath scanning
    requires jdk.management.agent; // required to run application with the com.sun.management.jmxremote property
    requires jdk.unsupported; // required by netty to allow reliable low-level API access to direct-buffers
    requires jdk.naming.dns; // required by netty dns libraries used by reactive postgres

    opens no.ssb.datasetaccess;

    exports no.ssb.datasetaccess;
}