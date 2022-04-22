package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.*;

import static io.grpc.Status.*;

public class ServerServiceImpl extends ServerServiceGrpc.ServerServiceImplBase {

	public static final String INVALID_SIGNATURE = "Invalid signature";
	private static final String DEADLINE_EXCEEDED_DESC = "Timed out!";
	private final Server server;
	private int faultsToTolerate;

	public ServerServiceImpl(int replicaID, int faultsToTolerate, String serverHostname, int serverBasePort) throws ServerInitializationFailedException {
		this.server = new Server(replicaID, faultsToTolerate, serverHostname, serverBasePort);
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
			Balance initialBalance = content.getBalance();
			ByteString balanceSignature = content.getBalanceSignature();
			ListSizes listSizes = content.getListSizes();
			ByteString listSizesSignature = content.getListSizesSignature();
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
			boolean isSender = content.getIsSender();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature((isSender)?senderKeyBS:receiverKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute the request
			BalanceRecord balanceRecord = server.readBalance((isSender)?senderKeyBS:receiverKeyBS);
			ListSizesRecord senderListSizesRecord = server.readListSizes(senderKeyBS);
			ListSizesRecord receiverListSizesRecord = server.readListSizes(receiverKeyBS);
			// Build Response
			ReadForWriteResponse.Builder builder = ReadForWriteResponse.newBuilder();
			builder.setBalance(balanceRecord.getBalance());
			builder.setBalanceSignature(balanceRecord.getSignature());
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
			byte[] serverSignature = getServerSignatureManager().sign(nonceToServer, response.toByteArray());
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
			ListSizes receiverListSizes = content.getReceiverListSizes();
			ByteString receiverSizesSignature = content.getReceiverSizesSignature();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(newTransfer.getSenderKey(), request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute Request
			server.sendAmount(newTransfer, senderTransferSignature, newBalance, balanceSignature, receiverListSizes, receiverSizesSignature);
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
		       InvalidTimestampException | InvalidTransferSignatureException | InvalidNewBalanceException |
		       InvalidNewListSizesException e) {
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
			// Check if the puzzle solution is correct as soon as possible to avoid unnecessary computation
			long puzzleSolution = content.getPuzzleSolution();
			if (!getServerSignatureManager().isPuzzleSolutionCorrect(publicKeyBS, puzzleSolution)) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Puzzle solution is incorrect").asRuntimeException());
				return;
			}
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(publicKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute the request
			BalanceRecord balanceRecord = server.readBalance(publicKeyBS);
			Balance balance = balanceRecord.getBalance();
			ByteString balanceSignature = balanceRecord.getSignature();
			ListSizesRecord listSizesRecord = server.readListSizes(publicKeyBS);
			ListSizes listSizes = listSizesRecord.getListSizes();
			ByteString listSizesSignature = listSizesRecord.getSignature();
			ByteString listSizesSigner = listSizesRecord.getSignerPublicKeyBS();
			TransfersRecord transfersRecord = server.getPendingIncomingTransfers(publicKeyBS);
			List<Transfer> pendingTransfers = transfersRecord.getTransfers();
			List<ByteString> senderSignatures = transfersRecord.getSenderSignatures();
			// Build Response
			CheckAccountResponse.Builder builder = CheckAccountResponse.newBuilder();
			builder.setBalance(balance);
			builder.setBalanceSignature(balanceSignature);
			builder.addAllPendingTransfers(pendingTransfers);
			builder.addAllSenderTransferSignatures(senderSignatures);
			builder.setListSizes(listSizes);
			builder.setListSizesSignature(listSizesSignature);
			builder.setListSizesSigner(listSizesSigner);
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
		catch (CypherFailedException | InvalidKeySpecException | NoSuchAlgorithmException |
		       SignatureVerificationFailedException | SQLException | AccountDoesNotHavePuzzleException e) {
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
			ListSizes senderListSizes = content.getSenderListSizes();
			ByteString senderSizesSignature = content.getSenderSizesSignature();
			ListSizes receiverListSizes = content.getReceiverListSizes();
			ByteString receiverSizesSignature = content.getReceiverSizesSignature();
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(transfer.getReceiverKey(), request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute Request
			server.receiveAmount(transfer, receiverTransferSignature, newBalance, balanceSignature, senderListSizes, senderSizesSignature, receiverListSizes, receiverSizesSignature);
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
		catch (TransferNotFoundException | InvalidNewBalanceException | InvalidTransferSignatureException |
		       InvalidNewListSizesException e) {
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
			// Check if the puzzle solution is correct as soon as possible to avoid unnecessary computation
			long puzzleSolution = content.getPuzzleSolution();
			if (!getServerSignatureManager().isPuzzleSolutionCorrect(publicKeyBS, puzzleSolution)) {
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Puzzle solution is incorrect").asRuntimeException());
				return;
			}
			byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
			if (!checkRequestSignature(publicKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
			// Execute the request
			TransfersRecord transfersRecord = server.getApprovedTransfers(publicKeyBS);
			List<Transfer> approvedTransfers = transfersRecord.getTransfers();
			List<ByteString> senderSignatures = transfersRecord.getSenderSignatures();
			List<ByteString> receiverSignatures = transfersRecord.getReceiverSignatures();
			ListSizesRecord listSizesRecord = server.readListSizes(publicKeyBS);
			ListSizes listSizes = listSizesRecord.getListSizes();
			ByteString sizesSignature = listSizesRecord.getSignature();
			ByteString sizesSigner = listSizesRecord.getSignerPublicKeyBS();
			// Build Response
			AuditResponse.Builder builder = AuditResponse.newBuilder();
			builder.addAllApprovedTransfers(approvedTransfers);
			builder.addAllSenderTransferSignatures(senderSignatures);
			builder.addAllReceiverTransferSignatures(receiverSignatures);
			builder.setListSizes(listSizes);
			builder.setListSizesSignature(sizesSignature);
			builder.setListSizesSigner(sizesSigner);
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
		catch (AccountDoesNotExistException | CypherFailedException | InvalidKeySpecException |
		       NoSuchAlgorithmException | SignatureVerificationFailedException | SQLException |
		       AccountDoesNotHavePuzzleException e) {
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
			Puzzle puzzle = getServerSignatureManager().generatePuzzle(publicKeyBS);
			// Build Response
			GetNonceResponse.Builder builder = GetNonceResponse.newBuilder();
			builder.setCypheredNonce(ByteString.copyFrom(nonce));
			builder.setPuzzle(puzzle);
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
	public void sendBroadcast(SignedSendBroadcastRequest request, StreamObserver<SignedSendBroadcastResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(DEADLINE_EXCEEDED.withDescription(DEADLINE_EXCEEDED_DESC).asRuntimeException());
			return;
		}
		try {
			//Check if there are any Identifiers with replicaID
			if(!server.requestIdentifiers.containsKey(request.getReplicaId()) ) {
				Map<String, RequestIdentifier> auxMap = new HashMap<String, RequestIdentifier>();
				auxMap.put(request.getSequenceNumber(), new RequestIdentifier(request.getReplicaId() , request.getSequenceNumber()));
				server.requestIdentifiers.put(request.getReplicaId(), auxMap);
			}
			//Check if there are any Identifiers with Sequence Number
			else if (!server.requestIdentifiers.get(request.getReplicaId()).containsKey(request.getSequenceNumber())) {

				server.requestIdentifiers.get(request.getReplicaId()).put(request.getSequenceNumber(), new RequestIdentifier(request.getReplicaId() , request.getSequenceNumber()));
			}

			//Send first Echo if not previously sent
			if (request.getBroadcastType() == 0) {
				//if (!checkRequestSignature(publicKeyBS, request.SignedRequest.getSignature, content.toByteArray(), responseObserver)) return;
				if (!server.requestIdentifiers.get(request.getReplicaId()).get(request.getSequenceNumber()).hasEchoed() ) {
					server.requestIdentifiers.get(request.getReplicaId()).get(request.getSequenceNumber()).receiveEcho(String.valueOf(server.replicaID), faultsToTolerate);
					server.requestIdentifiers.get(request.getReplicaId()).get(request.getSequenceNumber()).changeEchoed();
					server.sendEchoes(request.getWrappedRequest(), request.getReplicaId(), request.getSequenceNumber());
				}
			}
			//Send Ready if number of received Echoes reaches threshold ( N+F/2 +1)
			else if (request.getBroadcastType() == 1) {

				if (server.requestIdentifiers.get(request.getReplicaId()).get(request.getSequenceNumber()).receiveEcho(request.getSenderId(), this.faultsToTolerate) ) {
					server.requestIdentifiers.get(request.getReplicaId()).get(request.getSequenceNumber()).receiveReady(String.valueOf(server.replicaID), faultsToTolerate);

					server.sendReadies(request.getWrappedRequest(), request.getReplicaId(), request.getSequenceNumber());
				}
			}
			//Send Ready if number of received Readies reaches threshold (F+1) or send Deliver if it reaches another threshold (2*F +1)
			else if (request.getBroadcastType() == 2) {
				int rcvReadyResult = server.requestIdentifiers.get(request.getReplicaId()).get(request.getSequenceNumber()).receiveReady(request.getSenderId(), this.faultsToTolerate);
				if (rcvReadyResult == 1) {
					server.requestIdentifiers.get(request.getReplicaId()).get(request.getSequenceNumber()).receiveReady(String.valueOf(server.replicaID), faultsToTolerate);

					server.sendReadies(request.getWrappedRequest(), request.getReplicaId(), request.getSequenceNumber());
				}
				else if (rcvReadyResult == 2) {
					sendDelivery(request.getWrappedRequest(), request.getReplicaId(), request.getSequenceNumber(), responseObserver);
				}
			}

		}
		catch (Exception e/*CypherFailedException | NoSuchAlgorithmException | InvalidKeySpecException e*/) {
			responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	public SignedReceiveAmountRequest isSignedReceiveAmountRequest (BroadcastRequest req) {

		try {
			return req.getClientRequest().unpack(SignedReceiveAmountRequest.class);
		} catch (InvalidProtocolBufferException e) {
			return null;
		}
	}

	public SignedAuditRequest isSignedAuditRequest (BroadcastRequest req) {

		try {
			return req.getClientRequest().unpack(SignedAuditRequest.class);
		} catch (InvalidProtocolBufferException e) {
			return null;
		}
	}

	public SignedCheckAccountRequest isSignedCheckAccountRequest (BroadcastRequest req) {

		try {
			return req.getClientRequest().unpack(SignedCheckAccountRequest.class);
		} catch (InvalidProtocolBufferException e) {
			return null;
		}
	}

	public SignedSendAmountRequest isSignedSendAmountRequest (BroadcastRequest req) {

		try {
			return req.getClientRequest().unpack(SignedSendAmountRequest.class);
		} catch (InvalidProtocolBufferException e) {
			return null;
		}
	}

	public SignedReadForWriteRequest isSignedReadForWriteRequest (BroadcastRequest req) {

		try {
			return req.getClientRequest().unpack(SignedReadForWriteRequest.class);
		} catch (InvalidProtocolBufferException e) {
			return null;
		}
	}

	public void sendDelivery(BroadcastRequest payload, String ownerRepID, String sqcNumber, StreamObserver<?> responseObserver) {


		if (isSignedReceiveAmountRequest(payload) != null) {
			SignedReceiveAmountRequest request = isSignedReceiveAmountRequest(payload);
			try {
				// Parse Request & Check its Validity
				ReceiveAmountRequest content = request.getContent();
				Transfer transfer = content.getTransfer();
				ByteString receiverTransferSignature = content.getReceiverTransferSignature();
				Balance newBalance = content.getNewBalance();
				ByteString balanceSignature = content.getBalanceSignature();
				ListSizes senderListSizes = content.getSenderListSizes();
				ByteString senderSizesSignature = content.getSenderSizesSignature();
				ListSizes receiverListSizes = content.getReceiverListSizes();
				ByteString receiverSizesSignature = content.getReceiverSizesSignature();
				byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
				if (!checkRequestSignature(transfer.getReceiverKey(), request.getSignature(), content.toByteArray(), responseObserver))
					return;
				// Execute Request
				server.receiveAmount(transfer, receiverTransferSignature, newBalance, balanceSignature, senderListSizes, senderSizesSignature, receiverListSizes, receiverSizesSignature);
			} catch (Exception e) {

			}
		}
		else if(isSignedSendAmountRequest(payload) != null)  {
			SignedSendAmountRequest request = isSignedSendAmountRequest(payload);
			try {
				// Parse Request & Check its Validity
				SendAmountRequest content = request.getContent();
				Transfer newTransfer = content.getTransfer();
				ByteString senderTransferSignature = content.getSenderTransferSignature();
				Balance newBalance = content.getNewBalance();
				ByteString balanceSignature = content.getBalanceSignature();
				ListSizes receiverListSizes = content.getReceiverListSizes();
				ByteString receiverSizesSignature = content.getReceiverSizesSignature();
				byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
				if (!checkRequestSignature(newTransfer.getSenderKey(), request.getSignature(), content.toByteArray(), responseObserver)) return;
				// Execute Request
				server.sendAmount(newTransfer, senderTransferSignature, newBalance, balanceSignature, receiverListSizes, receiverSizesSignature);
			} catch (Exception e) {

			}
		}

		else if(isSignedAuditRequest(payload) != null) {
			SignedReadForWriteRequest request = isSignedReadForWriteRequest(payload);
			try {
				// Parse request & Check its Validity
				ReadForWriteRequest content = request.getContent();
				ByteString senderKeyBS = content.getSenderKey();
				ByteString receiverKeyBS = content.getReceiverKey();
				boolean isSender = content.getIsSender();
				byte[] cypheredNonceToServer = content.getCypheredNonce().toByteArray();
				if (!checkRequestSignature((isSender)?senderKeyBS:receiverKeyBS, request.getSignature(), content.toByteArray(), responseObserver)) return;
				// Execute the request
				BalanceRecord balanceRecord = server.readBalance((isSender)?senderKeyBS:receiverKeyBS);
				ListSizesRecord senderListSizesRecord = server.readListSizes(senderKeyBS);
				ListSizesRecord receiverListSizesRecord = server.readListSizes(receiverKeyBS);
			} catch (Exception e) {

			}
		}

	}

}
