package pt.tecnico.sec.bftb;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sec.bftb.client.Resources;
import pt.tecnico.sec.bftb.client.SignatureManager;
import pt.tecnico.sec.bftb.server.grpc.Server;

import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;

public class AuthIT extends BaseIT {

	@Test
	void clientInvalidSignatureTest() {
		try {
			long nonce = client.requestNonce();
			client.debugSabotageSignatureManager("wrong_user");
			StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> client.openAccount(nonce));
			assertTrue(INVALID_ARGUMENT.getCode() == e.getStatus().getCode() || INTERNAL.getCode() == e.getStatus().getCode());
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	@Test
	void clientWrongNonceTest() {
		try {
			long nonce = 0;
			StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> client.openAccount(nonce));
			assertSame(INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}
}
