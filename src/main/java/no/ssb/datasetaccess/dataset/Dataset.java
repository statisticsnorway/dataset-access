package no.ssb.datasetaccess.dataset;

import java.util.Objects;

public class Dataset {

    DatasetState state;
    Valuation valuation;

    public Dataset() {
    }

    public Dataset(DatasetState state, Valuation valuation) {
        this.state = state;
        this.valuation = valuation;
    }

    public DatasetState getState() {
        return state;
    }

    public void setState(DatasetState state) {
        this.state = state;
    }

    public Valuation getValuation() {
        return valuation;
    }

    public void setValuation(Valuation valuation) {
        this.valuation = valuation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dataset dataset = (Dataset) o;
        return state == dataset.state &&
                valuation == dataset.valuation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, valuation);
    }

    @Override
    public String toString() {
        return "Dataset{" +
                "state=" + state +
                ", valuation=" + valuation +
                '}';
    }
}
