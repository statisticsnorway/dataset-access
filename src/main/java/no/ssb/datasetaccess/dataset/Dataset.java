package no.ssb.datasetaccess.dataset;

import java.util.Objects;

public class Dataset {

    String datasetId;
    DatasetState state;
    Valuation valuation;

    public Dataset() {
    }

    public Dataset(String datasetId, DatasetState state, Valuation valuation) {
        this.datasetId = datasetId;
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

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dataset dataset = (Dataset) o;
        return datasetId.equals(dataset.datasetId) &&
                state == dataset.state &&
                valuation == dataset.valuation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(datasetId, state, valuation);
    }

    @Override
    public String toString() {
        return "Dataset{" +
                "datasetId='" + datasetId + '\'' +
                ", state=" + state +
                ", valuation=" + valuation +
                '}';
    }
}
