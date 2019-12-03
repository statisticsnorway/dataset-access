package no.ssb.datasetaccess.role;


import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;

public class RoleController implements Service {

    final RoleRepository repository;

    public RoleController(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{roleId}", (req, res) -> res.send(repository.getRole(req.path().param("roleId"))));

        rules.put("/{roleId}", Handler.create(Role.class, (req, res, role) -> {
            if (!req.path().param("roleId").equals(role.getRoleId())) {
                res.status(Http.Status.BAD_REQUEST_400).send("roleId in path must match that in body");
            }
            repository.createRole(role)
                    .doOnError(t -> res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage()))
                    .doOnComplete(() -> res.status(Http.Status.CREATED_201).send());
        }));

        rules.delete("/{roleId}", (req, res) -> repository.deleteRole(req.path().param("roleId"))
                .doOnError(t -> res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage()))
                .doOnComplete(() -> res.send()));
    }
}
