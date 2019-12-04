package no.ssb.datasetaccess.user;

import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;

public class UserService implements Service {

    final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{userId}", (req, res) -> repository.getUser(req.path().param("userId")).thenAccept(user -> res.send(user)));

        rules.put("/{userId}", Handler.create(User.class, (req, res, user) -> {
            if (!req.path().param("userId").equals(user.getUserId())) {
                res.status(Http.Status.BAD_REQUEST_400).send("userId in path must match that in body");
            }
            repository.createUser(user)
                    .thenRun(() -> res.status(Http.Status.CREATED_201).send())
                    .exceptionally(t -> {
                        res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                        return null;
                    });
        }));

        rules.delete("/{userId}", (req, res) -> repository.deleteUser(req.path().param("userId"))
                .thenRun(() -> res.send())
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                })
        );
    }
}
