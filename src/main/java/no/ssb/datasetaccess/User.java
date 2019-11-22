package no.ssb.datasetaccess;

public class User {

    private String id;

    public User(String id) {
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
        return id;
    }
}
