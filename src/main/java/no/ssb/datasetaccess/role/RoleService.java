package no.ssb.datasetaccess.role;


import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.ssb.dapla.auth.dataset.protobuf.Role;

import java.util.concurrent.TimeUnit;

public class RoleService implements Service {

    final RoleRepository repository;

    public RoleService(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{roleId}", this::doGet);
        rules.put("/{roleId}", Handler.create(Role.class, this::doPut));
        rules.delete("/{roleId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        String roleId = req.path().param("roleId");
        repository.getRole(roleId)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenAccept(role -> {
                    if (role == null) {
                        res.status(Http.Status.NOT_FOUND_404).send();
                    } else {
                        res.send(role);
                    }
                })
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                });
    }

    private void doPut(ServerRequest req, ServerResponse res, Role role) {
        String roleId = req.path().param("roleId");
        if (!roleId.equals(role.getRoleId())) {
            res.status(Http.Status.BAD_REQUEST_400).send("roleId in path must match that in body");
        }
        repository.createOrUpdateRole(role)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenRun(() -> {
                    res.headers().add("Location", "/role/" + roleId);
                    res.status(Http.Status.CREATED_201).send();
                })
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                });
    }

    private void doDelete(ServerRequest req, ServerResponse res) {
        String roleId = req.path().param("roleId");
        repository.deleteRole(roleId)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenRun(res::send)
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                });
    }
}
