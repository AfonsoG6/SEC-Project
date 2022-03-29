package pt.tecnico.sec.bftb;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sec.bftb.client.Resources;
import pt.tecnico.sec.bftb.server.grpc.Server;

import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;

public class AuthIT extends BaseIT {

	@Test
	void clientInvalidSignatureTest() {
		try {
			// Create request
			Server.OpenAccountRequest.Builder builder = Server.OpenAccountRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(Resources.getPublicKeyByUserId("user").getEncoded()));
			Server.OpenAccountRequest content = builder.build();
			Server.SignedOpenAccountRequest.Builder signedBuilder = Server.SignedOpenAccountRequest.newBuilder();
			signedBuilder.setContent(content);
			Server.SignedOpenAccountRequest signedRequest = signedBuilder.build();
			StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> client.debugSendRequest(signedRequest));
			assertTrue(INVALID_ARGUMENT.getCode() == e.getStatus().getCode() || INTERNAL.getCode() == e.getStatus().getCode());
		} catch (Exception e) {
			fail();
		}

	}

	@Test
	void serverInvalidSignatureTest() {
		// TODO
	}
}
