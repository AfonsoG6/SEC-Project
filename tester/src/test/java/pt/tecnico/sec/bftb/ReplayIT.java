package pt.tecnico.sec.bftb;

import com.google.protobuf.GeneratedMessageV3;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sec.bftb.server.grpc.Server.SignedReceiveAmountRequest;
import pt.tecnico.sec.bftb.server.grpc.Server.SignedSendAmountRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplayIT extends BaseIT {

	@Test
	void sendAmountRequestReplayedTest() {
		try {
			client.changeUser("user3");
			client.openAccount();
			client.changeUser("user4");
			client.openAccount();
			assertDoesNotThrow(() -> client.sendAmount("user3", 5));
			List<GeneratedMessageV3> requestHistory = client.getDebugRequestHistory();
			SignedSendAmountRequest sentRequest = (SignedSendAmountRequest) requestHistory.get(requestHistory.size() - 1);
			assertThrows(StatusRuntimeException.class, () -> client.debugSendRequest(sentRequest));
		}
		catch (Exception e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	@Test
	void receiveAmountRequestReplayedTest() {
		try {
			client.changeUser("user5");
			client.openAccount();
			client.changeUser("user6");
			client.openAccount();
			client.sendAmount("user5", 5);
			client.changeUser("user5");
			assertDoesNotThrow(() -> client.receiveAmount(0));
			List<GeneratedMessageV3> requestHistory = client.getDebugRequestHistory();
			SignedReceiveAmountRequest sentRequest = (SignedReceiveAmountRequest) requestHistory.get(requestHistory.size() - 1);
			assertThrows(StatusRuntimeException.class, () -> client.debugSendRequest(sentRequest));
		}
		catch (Exception e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}
}
