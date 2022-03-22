package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.bftb.server.exceptions.CypherFailedException;
import pt.tecnico.sec.bftb.server.grpc.Server.*;
import pt.tecnico.sec.bftb.server.grpc.ServerServiceGrpc;

import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import static io.grpc.Status.*;

public class ServerServiceImpl extends ServerServiceGrpc.ServerServiceImplBase {

	private static final String DEADLINE_EXCEEDED_DESC = "Timed out!";

	private final SignatureManager signatureManager;
	private final Server server;

	public ServerServiceImpl() throws NoSuchAlgorithmException {
		this.signatureManager = new SignatureManager();
		this.server = new Server();
    }

	@Override
	public void openAccount(SignedOpenAccountRequest request, StreamObserver<SignedOpenAccountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			OpenAccountRequest content = request.getContent();
			byte[] publicKeyBytes = content.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			long nonceToServer = signatureManager.decypherNonce(content.getCypheredNonce().toByteArray());
			byte[] clientSignature = request.getSignature().toByteArray();
			if (!signatureManager.verifySignature(publicKey, clientSignature, request.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid signature").asRuntimeException());
			}
			server.openAccount(publicKey);
			// Build Response
            SignedOpenAccountResponse.Builder signedBuilder = SignedOpenAccountResponse.newBuilder();
			byte[] serverSignature = signatureManager.sign(nonceToServer);
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
            SignedOpenAccountResponse response = signedBuilder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {
			// TODO handle exceptions correclty
		}
	}

	@Override
	public void sendAmount(SignedSendAmountRequest request, StreamObserver<SignedSendAmountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			SendAmountRequest content = request.getContent();
			byte[] sourceKeyBytes = content.getSourceKey().toByteArray();
			PublicKey sourceKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(sourceKeyBytes));
			byte[] destinationKeyBytes = content.getSourceKey().toByteArray();
			PublicKey destinationKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(destinationKeyBytes));
			int amount = content.getAmount();
			long nonceToServer = signatureManager.decypherNonce(content.getCypheredNonce().toByteArray());
			byte[] clientSignature = request.getSignature().toByteArray();
			if (!signatureManager.verifySignature(sourceKey, clientSignature, request.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid signature").asRuntimeException());
			}
			server.sendAmount(sourceKey, destinationKey, amount);
			// Build Response
			SignedSendAmountResponse.Builder signedBuilder = SignedSendAmountResponse.newBuilder();
			byte[] serverSignature = signatureManager.sign(nonceToServer);
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
			SignedSendAmountResponse response = signedBuilder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {
			// TODO handle exceptions correclty
		}
	}

	@Override
	public void checkAccount(SignedCheckAccountRequest request, StreamObserver<SignedCheckAccountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			CheckAccountRequest content = request.getContent();
			byte[] publicKeyBytes = content.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			long nonceToServer = signatureManager.decypherNonce(content.getCypheredNonce().toByteArray());
			byte[] clientSignature = request.getSignature().toByteArray();
			if (!signatureManager.verifySignature(publicKey, clientSignature, request.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid signature").asRuntimeException());
			}
			int balance = server.getBalance(publicKey);
			String transfers = server.getPendingTransfers(publicKey);
			// Build Response
            CheckAccountResponse.Builder builder = CheckAccountResponse.newBuilder();
			builder.setBalance(balance);
			builder.setPendingTransfers(transfers);

			SignedCheckAccountResponse.Builder signedBuilder = SignedCheckAccountResponse.newBuilder();
			signedBuilder.setContent(builder.build());
			byte[] serverSignature = signatureManager.sign(nonceToServer, signedBuilder.getContent().toByteArray());
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
			SignedCheckAccountResponse response = signedBuilder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {
			// TODO handle exceptions correclty
		}
	}

	@Override
	public void receiveAmount(SignedReceiveAmountRequest request, StreamObserver<SignedReceiveAmountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			ReceiveAmountRequest content = request.getContent();
			byte[] publicKeyBytes = content.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			long nonceToServer = signatureManager.decypherNonce(content.getCypheredNonce().toByteArray());
			byte[] clientSignature = request.getSignature().toByteArray();
			if (!signatureManager.verifySignature(publicKey, clientSignature, request.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid signature").asRuntimeException());
			}
			int transferNum = content.getTransferNum();
			server.receiveAmount(publicKey, transferNum);
			// Build Response
            SignedReceiveAmountResponse.Builder signedBuilder = SignedReceiveAmountResponse.newBuilder();
			byte[] serverSignature = signatureManager.sign(nonceToServer);
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
            SignedReceiveAmountResponse response = signedBuilder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {
			// TODO handle exceptions correclty
		}
	}

	@Override
	public void audit(SignedAuditRequest request, StreamObserver<SignedAuditResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			AuditRequest content = request.getContent();
			byte[] publicKeyBytes = content.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			long nonceToServer = signatureManager.decypherNonce(content.getCypheredNonce().toByteArray());
			byte[] clientSignature = request.getSignature().toByteArray();
			if (!signatureManager.verifySignature(publicKey, clientSignature, request.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid signature").asRuntimeException());
			}
			// Build Response
            AuditResponse.Builder builder = AuditResponse.newBuilder();
			builder.setHistory(server.getApprovedTransfers(publicKey));
			SignedAuditResponse.Builder signedBuilder = SignedAuditResponse.newBuilder();
			signedBuilder.setContent(builder.build());
			byte[] serverSignature = signatureManager.sign(nonceToServer, signedBuilder.getContent().toByteArray());
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
            SignedAuditResponse response = signedBuilder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (Exception e) {
			// TODO handle exceptions correclty
		}
	}

	@Override
	public void getNonce(GetNonceRequest request, StreamObserver<GetNonceResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			byte[] publicKeyBytes = request.getPublicKey().toByteArray();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			byte[] nonce = signatureManager.generateCypheredNonce(publicKey);
			// Build Response
            GetNonceResponse.Builder builder = GetNonceResponse.newBuilder();
			builder.setCypheredNonce(ByteString.copyFrom(nonce));
            GetNonceResponse response = builder.build();
            // Send Response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
		}
		catch (CypherFailedException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
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
