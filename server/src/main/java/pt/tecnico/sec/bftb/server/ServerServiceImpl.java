package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.bftb.server.exceptions.*;
import pt.tecnico.sec.bftb.grpc.Server.*;
import pt.tecnico.sec.bftb.grpc.ServerServiceGrpc;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;

import static io.grpc.Status.*;

public class ServerServiceImpl extends ServerServiceGrpc.ServerServiceImplBase {

	public static final String INVALID_SIGNATURE = "Invalid signature";
	private static final String DEADLINE_EXCEEDED_DESC = "Timed out!";
	private final SignatureManager signatureManager;
	private final Server server;

	public ServerServiceImpl() throws ServerInitializationFailedException {
		try {
			this.signatureManager = new SignatureManager();
			this.server = new Server(0);
		}
		catch (PrivateKeyLoadingFailedException e) {
			throw new ServerInitializationFailedException(e);
		}
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
			if (signatureManager.isSignatureInvalid(publicKey, clientSignature, content.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription(INVALID_SIGNATURE).asRuntimeException());
				return;
			}
			server.openAccount(publicKey);
			// Build Signed Response
			SignedOpenAccountResponse.Builder signedBuilder = SignedOpenAccountResponse.newBuilder();
			byte[] serverSignature = signatureManager.sign(nonceToServer);
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
			SignedOpenAccountResponse signedResponse = signedBuilder.build();
			// Send Response
			responseObserver.onNext(signedResponse);
			responseObserver.onCompleted();
		}
		catch (CypherFailedException | InvalidKeySpecException | NoSuchAlgorithmException | SignatureVerificationFailedException | SQLException e) {
			e.printStackTrace();
			responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
		catch (AccountAlreadyExistsException e) {
			e.printStackTrace();
			responseObserver.onError(ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
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
			long timestamp = content.getTimestamp();
			byte[] sourceKeyBytes = content.getSourceKey().toByteArray();
			PublicKey sourceKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(sourceKeyBytes));
			byte[] destinationKeyBytes = content.getDestinationKey().toByteArray();
			PublicKey destinationKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(destinationKeyBytes));
			int amount = content.getAmount();
			long nonceToServer = signatureManager.decypherNonce(content.getCypheredNonce().toByteArray());
			byte[] clientSignature = request.getSignature().toByteArray();
			if (signatureManager.isSignatureInvalid(sourceKey, clientSignature, content.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription(INVALID_SIGNATURE).asRuntimeException());
				return;
			}
			server.sendAmount(timestamp, sourceKey, destinationKey, amount);
			// Build Signed Response
			SignedSendAmountResponse.Builder signedBuilder = SignedSendAmountResponse.newBuilder();
			byte[] serverSignature = signatureManager.sign(nonceToServer);
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
			SignedSendAmountResponse signedResponse = signedBuilder.build();
			// Send Response
			responseObserver.onNext(signedResponse);
			responseObserver.onCompleted();
		}
		catch (CypherFailedException | InvalidKeySpecException | NoSuchAlgorithmException | SignatureVerificationFailedException | SQLException e) {
			e.printStackTrace();
			responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
		catch (AmountTooLowException | AccountDoesNotExistException | BalanceTooLowException | InvalidTimestampException e) {
			e.printStackTrace();
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
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
			if (signatureManager.isSignatureInvalid(publicKey, clientSignature, content.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription(INVALID_SIGNATURE).asRuntimeException());
				return;
			}
			// Build Response
			CheckAccountResponse.Builder builder = CheckAccountResponse.newBuilder();
			builder.setBalance(server.getAccountBalance(publicKey));
			builder.addAllPendingTransfers(server.getPendingIncomingTransfers(publicKey));
			CheckAccountResponse response = builder.build();
			// Build Signed Response
			SignedCheckAccountResponse.Builder signedBuilder = SignedCheckAccountResponse.newBuilder();
			signedBuilder.setContent(response);
			byte[] serverSignature = signatureManager.sign(nonceToServer, signedBuilder.getContent().toByteArray());
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
			SignedCheckAccountResponse signedResponse = signedBuilder.build();
			// Send Response
			responseObserver.onNext(signedResponse);
			responseObserver.onCompleted();
		}
		catch (CypherFailedException | InvalidKeySpecException | NoSuchAlgorithmException | SignatureVerificationFailedException | SQLException e) {
			e.printStackTrace();
			responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
		catch (AccountDoesNotExistException e) {
			e.printStackTrace();
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
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
			long timestamp = content.getTimestamp();
			byte[] sourceKeyBytes = content.getSourceKey().toByteArray();
			PublicKey sourceKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(sourceKeyBytes));
			byte[] destinationKeyBytes = content.getDestinationKey().toByteArray();
			PublicKey destinationKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(destinationKeyBytes));
			long nonceToServer = signatureManager.decypherNonce(content.getCypheredNonce().toByteArray());
			byte[] clientSignature = request.getSignature().toByteArray();
			if (signatureManager.isSignatureInvalid(destinationKey, clientSignature, content.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription(INVALID_SIGNATURE).asRuntimeException());
				return;
			}
			server.receiveAmount(timestamp, sourceKey, destinationKey);
			// Build Signed Response
			SignedReceiveAmountResponse.Builder signedBuilder = SignedReceiveAmountResponse.newBuilder();
			byte[] serverSignature = signatureManager.sign(nonceToServer);
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
			SignedReceiveAmountResponse signedResponse = signedBuilder.build();
			// Send Response
			responseObserver.onNext(signedResponse);
			responseObserver.onCompleted();
		}
		catch (CypherFailedException | InvalidKeySpecException | NoSuchAlgorithmException | SignatureVerificationFailedException | SQLException e) {
			e.printStackTrace();
			responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
		catch (AccountDoesNotExistException | BalanceTooLowException | TransferNotFoundException e) {
			e.printStackTrace();
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
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
			if (signatureManager.isSignatureInvalid(publicKey, clientSignature, content.toByteArray())) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription(INVALID_SIGNATURE).asRuntimeException());
				return;
			}
			// Build Response
			AuditResponse.Builder builder = AuditResponse.newBuilder();
			builder.addAllHistory(server.getApprovedTransfers(publicKey));
			AuditResponse response = builder.build();
			// Build Signed Response
			SignedAuditResponse.Builder signedBuilder = SignedAuditResponse.newBuilder();
			signedBuilder.setContent(response);
			byte[] serverSignature = signatureManager.sign(nonceToServer, signedBuilder.getContent().toByteArray());
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
			SignedAuditResponse signedResponse = signedBuilder.build();
			// Send Response
			responseObserver.onNext(signedResponse);
			responseObserver.onCompleted();
		}
		catch (AccountDoesNotExistException | CypherFailedException | InvalidKeySpecException | NoSuchAlgorithmException | SignatureVerificationFailedException | SQLException e) {
			e.printStackTrace();
			responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
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
