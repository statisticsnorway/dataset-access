package no.ssb.datasetaccess;

import no.ssb.helidon.application.DefaultHelidonApplicationBuilder;

import static java.util.Optional.ofNullable;

public class ApplicationBuilder extends DefaultHelidonApplicationBuilder {

    @Override
    public Application build() {
        return new Application(ofNullable(this.config).orElseGet(() -> createDefaultConfig()));
    }
}
