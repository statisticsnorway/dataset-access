package no.ssb.datasetaccess.autocreate.model;

import io.helidon.config.objectmapping.Value;
import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.DatasetStateSet;

import java.util.List;
import java.util.stream.Collectors;

public class States {

    private final List<String> includes;
    private final List<String> excludes;

    public States(@Value(key = "includes") List<String> includes, @Value(key = "excludes") List<String> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    public DatasetStateSet toSet() {
        DatasetStateSet.Builder datasetStateSet = DatasetStateSet.newBuilder();
        if (!includes.isEmpty()) {
            datasetStateSet.addAllIncludes(includes.stream().map(DatasetState::valueOf).collect(Collectors.toList()));
        }
        if (!excludes.isEmpty()) {
            datasetStateSet.addAllExcludes(excludes.stream().map(DatasetState::valueOf).collect(Collectors.toList()));
        }
        return datasetStateSet.build();
    }


    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }
}
