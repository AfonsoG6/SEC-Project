package pt.tecnico.sec.bftb.server;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.bftb.server.grpc.Server.*;
import pt.tecnico.sec.bftb.server.grpc.ServerServiceGrpc;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

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
		try {
			byte[] publicKeyBytes = request.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			// TODO: Verify signature
			server.openAccount(publicKey);
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
		try {
			byte[] sourceKeyBytes = request.getSourceKey().toByteArray();
			PublicKey sourceKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(sourceKeyBytes));
			byte[] destinationKeyBytes = request.getSourceKey().toByteArray();
			PublicKey destinationKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(destinationKeyBytes));
			int amount = request.getAmount();
			// TODO: Verify signature
			server.sendAmount(sourceKey, destinationKey, amount);
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
		try {
			byte[] publicKeyBytes = request.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			// TODO: Verify signature
			int balance = server.getBalance(publicKey);
			String transfers = server.getPendingTransfers(publicKey);
			// Build Response
            CheckAccountResponse.Builder builder = CheckAccountResponse.newBuilder();
			builder.setBalance(balance);
			builder.setPendingTransfers(transfers);
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
		try {
			byte[] publicKeyBytes = request.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			// TODO: Verify signature
			int transferNum = request.getTransferNum();
			server.receiveAmount(publicKey, transferNum);
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
		try {
			byte[] publicKeyBytes = request.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
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
	public void getNonce(GetNonceRequest request, StreamObserver<GetNonceResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription("Timed out!").asRuntimeException());
			return;
		}
		try {
			byte[] publicKeyBytes = request.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			// Build Response
            GetNonceResponse.Builder builder = GetNonceResponse.newBuilder();
            GetNonceResponse response = builder.build();
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
