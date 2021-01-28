package no.ssb.useraccess.autocreate.model;

import io.helidon.config.objectmapping.Value;

import java.util.List;

public class AutoCreate {
    private final List<AutoCreateItem> items;

    public AutoCreate(@Value(key = "autocreate") List<AutoCreateItem> items) {
        this.items = items;
    }

    public List<AutoCreateItem> getItems() {
        return items;
    }

}