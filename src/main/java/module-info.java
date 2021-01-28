import no.ssb.useraccess.UserAccessApplicationBuilder;
import no.ssb.helidon.application.HelidonApplicationBuilder;

module no.ssb.useraccess {
    requires org.slf4j;
    requires jul.to.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires io.helidon.webserver;
    requires io.helidon.webserver.accesslog;
    requires io.helidon.config;
    requires io.helidon.config.objectmapping;
    requires java.net.http;
    requires io.helidon.common.reactive;
    requires org.flywaydb.core;
    requires org.postgresql.jdbc;
    requires logback.classic;
    requires io.helidon.metrics;
    requires io.helidon.health;
    requires io.helidon.health.checks;
    requires io.helidon.tracing;

    requires no.ssb.helidon.media.protobuf.json.server;
    requires java.logging;
    requires io.helidon.dbclient;

    /*
     * Not so well documented requirements are declared here to force fail-fast with proper error message if
     * missing from jvm.
     */

    requires jdk.unsupported; // required by netty to allow reliable low-level API access to direct-buffers
    requires jdk.naming.dns; // required by netty dns libraries used by reactive postgres
    requires java.sql; // required by flyway
    requires io.helidon.microprofile.config; // metrics uses provider org.eclipse.microprofile.config.spi.ConfigProviderResolver
    requires com.google.protobuf.util;
    requires no.ssb.helidon.application;
    requires io.helidon.dbclient.health;
    requires com.google.protobuf;
    requires no.ssb.dapla.auth.dataset.protobuf;

    opens db.migration; // flyway needs this to read migration files

    exports no.ssb.useraccess; // allows running individual integration tests from IntelliJ
    exports no.ssb.useraccess.autocreate.model;
    exports no.ssb.useraccess.token to com.fasterxml.jackson.databind;

    provides HelidonApplicationBuilder with UserAccessApplicationBuilder;
}