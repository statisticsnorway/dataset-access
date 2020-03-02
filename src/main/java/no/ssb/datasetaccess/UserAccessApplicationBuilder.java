package no.ssb.datasetaccess;

import no.ssb.helidon.application.DefaultHelidonApplicationBuilder;

import static java.util.Optional.ofNullable;

public class UserAccessApplicationBuilder extends DefaultHelidonApplicationBuilder {

    @Override
    public UserAccessApplication build() {
        return new UserAccessApplication(ofNullable(this.config).orElseGet(() -> createDefaultConfig()));
    }
}
