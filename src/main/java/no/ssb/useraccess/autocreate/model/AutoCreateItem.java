package no.ssb.useraccess.autocreate.model;


import io.helidon.config.objectmapping.Value;

import java.util.List;


public class AutoCreateItem {
    private final Domain domain;
    private final User user;
    private final List<Role> roles;

    public AutoCreateItem(
            @Value(key = "domain") Domain domain,
            @Value(key = "user") User user,
            @Value(key = "roles") List<Role> roles) {
        this.domain = domain;
        this.user = user;
        this.roles = roles;
    }

    public Domain getDomain() {
        return this.domain;
    }

    public User getUser() {
        return user;
    }

    public List<Role> getRoles() {
        return roles;
    }
}
