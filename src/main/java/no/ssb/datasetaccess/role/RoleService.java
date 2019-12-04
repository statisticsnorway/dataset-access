package no.ssb.datasetaccess.role;


import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;

public class RoleService implements Service {

    final RoleRepository repository;

    public RoleService(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{roleId}", (req, res) -> repository.getRole(req.path().param("roleId")).thenAccept(role -> res.send(role)));

        rules.put("/{roleId}", Handler.create(Role.class, (req, res, role) -> {
            if (!req.path().param("roleId").equals(role.getRoleId())) {
                res.status(Http.Status.BAD_REQUEST_400).send("roleId in path must match that in body");
            }
            repository.createRole(role)
                    .thenRun(() -> res.status(Http.Status.CREATED_201).send())
                    .exceptionally(t -> {
                        res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                        return null;
                    });
        }));

        rules.delete("/{roleId}", (req, res) -> repository.deleteRole(req.path().param("roleId"))
                .thenRun(() -> res.send())
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                })
        );
    }
}
