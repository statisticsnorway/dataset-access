module no.ssb.datasetaccess {
    requires org.slf4j;
    requires org.reactivestreams;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires vertx.core;
    requires vertx.pg.client;
    requires vertx.sql.client;
    requires io.helidon.webserver;
    requires io.helidon.config;
    requires io.helidon.media.jackson.server;
    requires java.net.http;
    requires io.helidon.common.reactive;
    requires org.flywaydb.core;

    /*
     * Not so well documented requirements are declared here to force fail-fast with proper error message if
     * missing from jvm.
     */
    //requires jdk.management.agent; // required to run application with the com.sun.management.jmxremote property
    requires jdk.unsupported; // required by netty to allow reliable low-level API access to direct-buffers
    requires jdk.naming.dns; // required by netty dns libraries used by reactive postgres

    opens no.ssb.datasetaccess to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.access to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.dataset to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.role to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.user to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.token to com.fasterxml.jackson.databind;

    opens db.migration;
}