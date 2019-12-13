package no.ssb.datasetaccess.dataset;

public enum Valuation {
    OPEN(1),
    INTERNAL(2),
    SHIELDED(3),
    SENSITIVE(4);

    private final int level;

    private Valuation(int level) {
        this.level = level;
    }

    public boolean grantsAccessTo(Valuation other) {
        return level >= other.level;
    }
}
