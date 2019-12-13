package no.ssb.datasetaccess.user;

import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

public class UserService implements Service {

    final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{userId}", this::doGet);
        rules.put("/{userId}", Handler.create(User.class, this::doPut));
        rules.delete("/{userId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        String userId = req.path().param("userId");
        repository.getUser(userId)
                .thenAccept(user -> {
                    if (user == null) {
                        res.status(Http.Status.NOT_FOUND_404).send();
                    } else {
                        res.send(user);
                    }
                });
    }

    private void doPut(ServerRequest req, ServerResponse res, User user) {
        String userId = req.path().param("userId");
        if (!userId.equals(user.getUserId())) {
            res.status(Http.Status.BAD_REQUEST_400).send("userId in path must match that in body");
        }
        repository.createUser(user)
                .thenRun(() -> {
                    res.headers().add("Location", "/user/" + userId);
                    res.status(Http.Status.CREATED_201).send();
                })
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                });
    }

    private void doDelete(ServerRequest req, ServerResponse res) {
        String userId = req.path().param("userId");
        repository.deleteUser(userId)
                .thenRun(res::send)
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                });
    }
}
