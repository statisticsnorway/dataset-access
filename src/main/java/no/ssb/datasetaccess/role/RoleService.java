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
        rules.get("/{roleId}", getRoleHandler());
        rules.put("/{roleId}", putRoleHandler());
        rules.delete("/{roleId}", deleteRoleHandler());
    }

    private Handler getRoleHandler() {
        return (req, res) -> {
            String roleId = req.path().param("roleId");
            repository.getRole(roleId)
                    .thenAccept(role -> {
                        if (role == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            res.send(role);
                        }
                    });
        };
    }

    private Handler putRoleHandler() {
        return Handler.create(Role.class, (req, res, role) -> {
            String roleId = req.path().param("roleId");
            if (!roleId.equals(role.getRoleId())) {
                res.status(Http.Status.BAD_REQUEST_400).send("roleId in path must match that in body");
            }
            repository.createRole(role)
                    .thenRun(() -> {
                        res.headers().add("Location", "/role/" + roleId);
                        res.status(Http.Status.CREATED_201).send();
                    })
                    .exceptionally(t -> {
                        res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                        return null;
                    });
        });
    }

    private Handler deleteRoleHandler() {
        return (req, res) -> {
            String roleId = req.path().param("roleId");
            repository.deleteRole(roleId)
                    .thenRun(res::send)
                    .exceptionally(t -> {
                        res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                        return null;
                    });
        };
    }
}
