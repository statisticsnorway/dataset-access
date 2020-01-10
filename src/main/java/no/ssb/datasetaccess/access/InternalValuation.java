package no.ssb.datasetaccess.access;

enum InternalValuation {
    OPEN(1),
    INTERNAL(2),
    SHIELDED(3),
    SENSITIVE(4);

    private final int level;

    private InternalValuation(int level) {
        this.level = level;
    }

    boolean grantsAccessTo(InternalValuation other) {
        return level >= other.level;
    }
}
