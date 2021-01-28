package no.ssb.useraccess.autocreate.model;

import io.helidon.config.objectmapping.Value;
import no.ssb.dapla.auth.dataset.protobuf.PathSet;
import no.ssb.useraccess.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;


public class Paths {
    private final List<String> pathIncludes;
    private final List<String> pathExcludes;

    public Paths(@Value(key = "includes") List<String> pathIncludes,
                 @Value(key = "excludes") List<String> pathExcludes) {
        this.pathIncludes = pathIncludes;
        this.pathExcludes = pathExcludes;
    }

    public PathSet toPathSet(String userId) {
        PathSet.Builder paths = PathSet.newBuilder();
        if (!pathIncludes.isEmpty() && !pathIncludes.get(0).equals("")) {
            paths.addAllIncludes(
                    pathIncludes.stream().map(include ->
                         StringUtil.substitudeVariable(include, userId)
                    ).collect(Collectors.toList())
            );
        }
        if (!pathExcludes.isEmpty() && !pathExcludes.get(0).equals("")) {
            paths.addAllExcludes(
                    pathExcludes.stream().map(exclude ->
                         StringUtil.substitudeVariable(exclude, userId)
                    ).collect(Collectors.toList())
            );
        }
        return paths.build();
    }

    public List<String> getPathIncludes() {
        return pathIncludes;
    }

    public List<String> getPathExcludes() {
        return pathExcludes;
    }
}
