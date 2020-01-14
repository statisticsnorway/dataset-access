module no.ssb.datasetaccess {
    requires org.slf4j;
    requires jul.to.slf4j;
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
    requires org.postgresql.jdbc;
    requires logback.classic;
    requires io.helidon.metrics;
    requires io.helidon.health;
    requires io.helidon.health.checks;

    requires no.ssb.dapla.auth.dataset.protobuf;
    requires no.ssb.helidon.media.protobuf.json.server;
    requires grpc.protobuf;
    requires io.helidon.grpc.server;
    requires java.logging;

    /*
     * Not so well documented requirements are declared here to force fail-fast with proper error message if
     * missing from jvm.
     */

    requires jdk.unsupported; // required by netty to allow reliable low-level API access to direct-buffers
    requires jdk.naming.dns; // required by netty dns libraries used by reactive postgres
    requires java.sql; // required by flyway
    requires io.helidon.microprofile.config; // metrics uses provider org.eclipse.microprofile.config.spi.ConfigProviderResolver
    requires perfmark.api; // needed by grpc-client
    requires javax.inject;
    requires com.google.protobuf.util; // required by io.helidon.grpc.server

    opens no.ssb.datasetaccess to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.access to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.role to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.user to com.fasterxml.jackson.databind;
    opens no.ssb.datasetaccess.token to com.fasterxml.jackson.databind;

    opens db.migration; // flyway needs this to read migration files

    exports no.ssb.datasetaccess; // allows running individual integration tests from IntelliJ
}