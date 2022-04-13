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

	public ServerServiceImpl(int replicaID) throws ServerInitializationFailedException {
		try {
			this.signatureManager = new SignatureManager(replicaID);
			this.server = new Server(replicaID);
		}
		catch (PrivateKeyLoadingFailedException e) {
			throw new ServerInitializationFailedException(e);
		}
	}

	private boolean checkRequestSignature(ByteString publicKeyBS, ByteString signature, byte[] content, StreamObserver<?> responseObserver)
			throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureVerificationFailedException {
		PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBS.toByteArray()));
		if (signatureManager.isSignatureInvalid(publicKey, signature.toByteArray(), content)) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription(INVALID_SIGNATURE).asRuntimeException());
			return false;
		}
		else return true;
	}

	@Override
	public void openAccount(SignedOpenAccountRequest request, StreamObserver<SignedOpenAccountResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			// Parse request & Check its Validity
			OpenAccountRequest content = request.getContent();
			ByteString publicKeyBS = content.getPublicKey();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(publicKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute the request
			server.openAccount(publicKeyBS);
			// Build Signed Response
			SignedOpenAccountResponse.Builder signedBuilder = SignedOpenAccountResponse.newBuilder();
			long nonceToServer = signatureManager.decypherNonce(cypheredNonceToServer);
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
			// Parse Request & Check its Validity
			SendAmountRequest content = request.getContent();
			Transfer newTransfer = content.getTransfer();
			ByteString senderSignature = content.getSenderSignature();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(newTransfer.getSenderKey(), request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute Request
			server.sendAmount(newTransfer, senderSignature);
			// Build Signed Response
			SignedSendAmountResponse.Builder signedBuilder = SignedSendAmountResponse.newBuilder();
			long nonceToServer = signatureManager.decypherNonce(cypheredNonceToServer);
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
			// Parse Request & Check its Validity
			CheckAccountRequest content = request.getContent();
			ByteString publicKeyBS = content.getPublicKey();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(publicKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute the request & Build Response
			CheckAccountResponse.Builder builder = CheckAccountResponse.newBuilder();
			builder.setBalance(server.getAccountBalance(publicKeyBS));
			builder.addAllPendingTransfers(server.getPendingIncomingTransfers(publicKeyBS));
			CheckAccountResponse response = builder.build();
			// Build Signed Response
			SignedCheckAccountResponse.Builder signedBuilder = SignedCheckAccountResponse.newBuilder();
			signedBuilder.setContent(response);
			long nonceToServer = signatureManager.decypherNonce(cypheredNonceToServer);
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
			// Parse Request & Check its Validity
			ReceiveAmountRequest content = request.getContent();
			Transfer transfer = content.getTransfer();
			ByteString receiverSignature = content.getReceiverSignature();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(transfer.getReceiverKey(), request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute Request
			server.receiveAmount(transfer, receiverSignature);
			// Build Signed Response
			SignedReceiveAmountResponse.Builder signedBuilder = SignedReceiveAmountResponse.newBuilder();
			long nonceToServer = signatureManager.decypherNonce(cypheredNonceToServer);
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
			// Parse Request & Check its Validity
			AuditRequest content = request.getContent();
			ByteString publicKeyBS = content.getPublicKey();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(publicKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute the request & Build Response
			AuditResponse.Builder builder = AuditResponse.newBuilder();
			builder.addAllHistory(server.getApprovedTransfers(publicKeyBS));
			AuditResponse response = builder.build();
			// Build Signed Response
			SignedAuditResponse.Builder signedBuilder = SignedAuditResponse.newBuilder();
			signedBuilder.setContent(response);
			long nonceToServer = signatureManager.decypherNonce(cypheredNonceToServer);
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
			// Parse Request
			ByteString publicKeyBS = request.getPublicKey();
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBS.toByteArray()));
			// Execute the request
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
}
