package pt.tecnico.sec.server;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.server.grpc.ServerServiceGrpc;
import pt.tecnico.sec.server.grpc.Server.*;

import java.security.PublicKey;

import static io.grpc.Status.DEADLINE_EXCEEDED;
import static io.grpc.Status.INVALID_ARGUMENT;

public class ServerServiceImpl extends ServerServiceGrpc.ServerServiceImplBase {

	private final Server server;

	public ServerServiceImpl() {
		this.server = new Server();
    }

	@Override
	public void openAccount(OpenAccountRequest request, StreamObserver<OpenAccountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription("Timed out!").asRuntimeException());
			return;
		}
		String publicKey = request.getPublicKey();
		// convert to PublicKey
		try {
			// Build Response
            OpenAccountResponse.Builder builder = OpenAccountResponse.newBuilder();
            OpenAccountResponse response = builder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {

		}
	}

	@Override
	public void sendAmount(SendAmountRequest request, StreamObserver<SendAmountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription("Timed out!").asRuntimeException());
			return;
		}
		String sourceKey = request.getSourceKey();
		String destinationKey = request.getDestinationKey();
		// convert to PublicKey
		try {
			// Build Response
            SendAmountResponse.Builder builder = SendAmountResponse.newBuilder();
            SendAmountResponse response = builder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {

		}
	}

	@Override
	public void checkAccount(CheckAccountRequest request, StreamObserver<CheckAccountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription("Timed out!").asRuntimeException());
			return;
		}
		String publicKey = request.getPublicKey();
		// convert to PublicKey
		try {
			// Build Response
            CheckAccountResponse.Builder builder = CheckAccountResponse.newBuilder();
            CheckAccountResponse response = builder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {

		}
	}

	@Override
	public void receiveAmount(ReceiveAmountRequest request, StreamObserver<ReceiveAmountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription("Timed out!").asRuntimeException());
			return;
		}
		String publicKey = request.getPublicKey();
		// convert to PublicKey
		try {
			// Build Response
            ReceiveAmountResponse.Builder builder = ReceiveAmountResponse.newBuilder();
            ReceiveAmountResponse response = builder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {

		}
	}

	@Override
	public void audit(AuditRequest request, StreamObserver<AuditResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription("Timed out!").asRuntimeException());
			return;
		}
		String publicKey = request.getPublicKey();
		// convert to PublicKey
		try {
			// Build Response
            AuditResponse.Builder builder = AuditResponse.newBuilder();
            AuditResponse response = builder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {

		}
	}

	@Override
	public void operation(OperationRequest request, StreamObserver<OperationResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription("Timed out!").asRuntimeException());
			return;
		}
		String publicKey = request.getPublicKey();
		// convert to PublicKey
		try {
			// Build Response
            OperationResponse.Builder builder = OperationResponse.newBuilder();
            OperationResponse response = builder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {

		}
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
