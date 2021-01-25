package no.ssb.datasetaccess.autocreate.model;

import io.helidon.config.objectmapping.Value;

public class MaxValuation {
    private final String maxValuation;

    public MaxValuation(@Value(key = "maxValuation") String maxValuation) {
        this.maxValuation = maxValuation;
    }

    public String getMaxValuation() {
        return maxValuation;
    }
}
