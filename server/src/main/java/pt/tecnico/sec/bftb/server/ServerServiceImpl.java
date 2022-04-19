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
	private final Server server;

	public ServerServiceImpl(int replicaID) throws ServerInitializationFailedException {
		this.server = new Server(replicaID);
	}

	SignatureManager getServerSignatureManager() {
		return server.getSignatureManager();
	}

	private boolean checkRequestSignature(ByteString publicKeyBS, ByteString signature, byte[] content, StreamObserver<?> responseObserver)
			throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureVerificationFailedException {
		PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBS.toByteArray()));
		if (getServerSignatureManager().isNonceSignatureInvalid(publicKey, signature.toByteArray(), content)) {
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
			Balance initialBalance = content.getInitialBalance();
			ByteString balanceSignature = content.getBalanceSignature();
			ListSizes listSizes = content.getListSizes();
			ByteString listSizesSignature = content.getSizesSignature();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(publicKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute the request
			server.openAccount(publicKeyBS, initialBalance, balanceSignature, listSizes, listSizesSignature);
			// Build Signed Response
			SignedOpenAccountResponse.Builder signedBuilder = SignedOpenAccountResponse.newBuilder();
			long nonceToServer = getServerSignatureManager().decypherNonce(cypheredNonceToServer);
			byte[] serverSignature = getServerSignatureManager().sign(nonceToServer);
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
		catch (InvalidNewBalanceException | InvalidNewListSizesException e) {
			e.printStackTrace();
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void readForWrite(SignedReadForWriteRequest request, StreamObserver<SignedReadForWriteResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			// Parse request & Check its Validity
			ReadForWriteRequest content = request.getContent();
			ByteString senderKeyBS = content.getSenderKey();
			ByteString receiverKeyBS = content.getReceiverKey();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(senderKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute the request
			BalanceRecord balanceRecord = server.readBalanceForWrite(senderKeyBS);
			ListSizesRecord senderListSizesRecord = server.readListSizesForWrite(senderKeyBS);
			ListSizesRecord receiverListSizesRecord = server.readListSizesForWrite(receiverKeyBS);
			// Build Response
			ReadForWriteResponse.Builder builder = ReadForWriteResponse.newBuilder();
			builder.setBalance(balanceRecord.getBalance());
			builder.setBalanceSignature(ByteString.copyFrom(balanceRecord.getSignature()));
			builder.setSenderListSizes(senderListSizesRecord.getListSizes());
			builder.setSenderListSizesSignature(senderListSizesRecord.getSignature());
			builder.setSenderListSizesSigner(senderListSizesRecord.getSignerPublicKeyBS());
			builder.setReceiverListSizes(receiverListSizesRecord.getListSizes());
			builder.setReceiverListSizesSignature(receiverListSizesRecord.getSignature());
			builder.setReceiverListSizesSigner(receiverListSizesRecord.getSignerPublicKeyBS());
			ReadForWriteResponse response = builder.build();
			// Build Signed Response
			SignedReadForWriteResponse.Builder signedBuilder = SignedReadForWriteResponse.newBuilder();
			signedBuilder.setContent(response);
			long nonceToServer = getServerSignatureManager().decypherNonce(cypheredNonceToServer);
			byte[] serverSignature = getServerSignatureManager().sign(nonceToServer);
			signedBuilder.setSignature(ByteString.copyFrom(serverSignature));
			SignedReadForWriteResponse signedResponse = signedBuilder.build();
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
			ByteString senderTransferSignature = content.getSenderTransferSignature();
			Balance newBalance = content.getNewBalance();
			ByteString balanceSignature = content.getBalanceSignature();
			// TODO: Treat List sizes
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(newTransfer.getSenderKey(), request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute Request
			server.sendAmount(newTransfer, senderTransferSignature, newBalance, balanceSignature);
			// Build Signed Response
			SignedSendAmountResponse.Builder signedBuilder = SignedSendAmountResponse.newBuilder();
			long nonceToServer = getServerSignatureManager().decypherNonce(cypheredNonceToServer);
			byte[] serverSignature = getServerSignatureManager().sign(nonceToServer);
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
		catch (AmountTooLowException | AccountDoesNotExistException | BalanceTooLowException |
		       InvalidTimestampException | InvalidTransferSignatureException | InvalidNewBalanceException e) {
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
			long nonceToServer = getServerSignatureManager().decypherNonce(cypheredNonceToServer);
			byte[] serverSignature = getServerSignatureManager().sign(nonceToServer, signedBuilder.getContent().toByteArray());
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
			ByteString receiverTransferSignature = content.getReceiverTransferSignature();
			Balance newBalance = content.getNewBalance();
			ByteString balanceSignature = content.getBalanceSignature();
			// TODO: Treat List sizes
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(transfer.getReceiverKey(), request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute Request
			server.receiveAmount(transfer, receiverTransferSignature, newBalance, balanceSignature);
			// Build Signed Response
			SignedReceiveAmountResponse.Builder signedBuilder = SignedReceiveAmountResponse.newBuilder();
			long nonceToServer = getServerSignatureManager().decypherNonce(cypheredNonceToServer);
			byte[] serverSignature = getServerSignatureManager().sign(nonceToServer);
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
		catch (TransferNotFoundException | InvalidNewBalanceException | InvalidTransferSignatureException e) {
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
			long nonceToServer = getServerSignatureManager().decypherNonce(cypheredNonceToServer);
			byte[] serverSignature = getServerSignatureManager().sign(nonceToServer, signedBuilder.getContent().toByteArray());
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
			byte[] nonce = getServerSignatureManager().generateCypheredNonce(publicKey);
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
