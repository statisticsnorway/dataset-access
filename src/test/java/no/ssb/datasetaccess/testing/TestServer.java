package no.ssb.datasetaccess.testing;

import no.ssb.datasetaccess.Application;

public class TestServer {

    final Application application = new Application();

    public TestClient client() {
        return TestClient.newClient("localhost", application.getWebServer().port());
    }

    public Application getApplication() {
        return application;
    }

    public void shutdown() {
        application.getWebServer().shutdown();
    }
}
