package no.ssb.datasetaccess.access;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.helidon.grpc.server.GrpcServer;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckRequest;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckResponse;
import no.ssb.dapla.auth.dataset.protobuf.AuthServiceGrpc;
import no.ssb.datasetaccess.Application;
import no.ssb.datasetaccess.IntegrationTestExtension;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.role.Privilege;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
class AccessServiceGrpcTest {

    @Inject
    Application application;

    //@Test
    public void thatHasAccessWorks() {
        int grpcPort = application.get(GrpcServer.class).port();
        Channel channel = ManagedChannelBuilder.forAddress("127.0.0.1", grpcPort).usePlaintext().build();
        AuthServiceGrpc.AuthServiceBlockingStub client = AuthServiceGrpc.newBlockingStub(channel);

        AccessCheckResponse response = client.hasAccess(AccessCheckRequest.newBuilder()
                .setUserId("john")
                .setPrivilege(Privilege.READ.name())
                .setNamespace("/a/b/c")
                .setValuation(Valuation.OPEN.name())
                .setState(DatasetState.INPUT.name())
                .build());
        assertTrue(response.getAllowed());
    }
}
