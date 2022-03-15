package pt.tecnico.sec.server;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.server.grpc.ServerServiceGrpc;
import pt.tecnico.sec.server.grpc.Server.*;

import static io.grpc.Status.DEADLINE_EXCEEDED;
import static io.grpc.Status.INVALID_ARGUMENT;

public class ServerServiceImpl extends ServerServiceGrpc.ServerServiceImplBase {

	private final Server server;

	public ServerServiceImpl() {
		this.server = new Server();
    }

	@Override
	public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription("Timed out!").asRuntimeException());
			return;
		}
		String input = request.getInput();
		if (input.isBlank()) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty!").asRuntimeException());
			return;
		}
		String output = "Hello " + input + "!";
		// Build response
		PingResponse.Builder builder = PingResponse.newBuilder();
		builder.setOutput(output);
		PingResponse response = builder.build();
		// Send Response
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

}
