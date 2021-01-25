package no.ssb.datasetaccess.autocreate.model;

import io.helidon.config.objectmapping.Value;
import no.ssb.datasetaccess.util.StringUtil;

public class Description {
    private final String description;

    public Description(@Value(key = "description") String description) {
        this.description = description;
    }

    public String getDescription(String userId) {
        return StringUtil.substitudeVariable(description, userId);
    }
}
