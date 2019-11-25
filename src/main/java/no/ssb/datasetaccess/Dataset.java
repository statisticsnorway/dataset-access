package no.ssb.datasetaccess;

public class Dataset {

    private String id;

    public Dataset(String id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "Dataset{" +
                "id='" + id + '\'' +
                '}';
    }
}
