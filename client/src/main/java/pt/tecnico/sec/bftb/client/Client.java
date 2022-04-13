package pt.tecnico.sec.bftb.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sec.bftb.client.exceptions.*;
import pt.tecnico.sec.bftb.grpc.Server.*;
import pt.tecnico.sec.bftb.grpc.ServerServiceGrpc;
import pt.tecnico.sec.bftb.grpc.ServerServiceGrpc.*;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Client {

	public static final String SERVER_ERROR_PREFIX = "SERVER ERROR: ";
	public static final String ERROR_PREFIX = "ERROR: ";
	public static final String OPERATION_SUCCESSFUL = "Operation successful!";
	public static final String OPERATION_FAILED = "Operation failed!";
	private static final long DEADLINE_SEC = 10;    // Timeout deadline in seconds
	private final ConcurrentHashMap<Integer, ServerServiceBlockingStub> stubs;
	private final SignatureManager signatureManager;
	private final Map<Integer, PublicKey> serverPublicKeys;
	private PublicKey userPublicKey;
	private PrivateKey userPrivateKey;
	private List<Transfer> lastCheckAccountTransfers = null;
	private final int faultsToTolerate;
	private final int numberOfServerReplicas;
	private final List<GeneratedMessageV3> debugRequestHistory = new LinkedList<>();

	public Client(String serverHostname, int serverBasePort, int faultsToTolerate)
			throws CertificateException, KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		this.stubs = new ConcurrentHashMap<>();
		this.faultsToTolerate = faultsToTolerate;
		this.numberOfServerReplicas = (3 * faultsToTolerate) + 1;
		for (int i = 0; i < numberOfServerReplicas; i++) {
			int replicaPort = serverBasePort + i;
			String replicaURI = String.format("%s:%d", serverHostname, replicaPort);
			ManagedChannel channel = ManagedChannelBuilder.forTarget(replicaURI).usePlaintext().build();
			this.stubs.put(i, ServerServiceGrpc.newBlockingStub(channel));
		}
		// Default user ID is "user", just for simplicity
		Resources.init();
		this.userPublicKey = Resources.getPublicKeyByUserId("user");
		this.userPrivateKey = Resources.getPrivateKeyByUserId("user");
		this.signatureManager = new SignatureManager(this.userPrivateKey);
		this.serverPublicKeys = new HashMap<>();
		for (int i = 0; i < numberOfServerReplicas; i++) {
			serverPublicKeys.put(i, Resources.getServerReplicaPublicKey(i));
		}
	}

	private void printNumAcks(int numAcks) {
		System.out.println("Number of ACKS: " + numAcks + "/" + this.numberOfServerReplicas);
	}

	public List<GeneratedMessageV3> getDebugRequestHistory() {
		return debugRequestHistory;
	}

	public void debugSabotageSignatureManager(String userId)
			throws KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		signatureManager.setPrivateKey(Resources.getPrivateKeyByUserId(userId));
	}

	public void changeUser(String userId) throws KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		userPrivateKey = Resources.getPrivateKeyByUserId(userId);
		userPublicKey = Resources.getPublicKeyByUserId(userId);
		signatureManager.setPrivateKey(userPrivateKey);
		lastCheckAccountTransfers = null;
		System.out.printf("User changed to '%s'%n", userId);
	}

	public long requestNonce(ServerServiceBlockingStub stub) throws NonceRequestFailedException {
		try {
			GetNonceRequest request = GetNonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(userPublicKey.getEncoded())).build();
			debugRequestHistory.add(request);
			GetNonceResponse response = stub.getNonce(request);
			byte[] cypheredNonce = response.getCypheredNonce().toByteArray();
			return signatureManager.decypherNonce(cypheredNonce);
		}
		catch (StatusRuntimeException e) {
			System.out.println(SERVER_ERROR_PREFIX + e.getMessage());
			throw new NonceRequestFailedException(e);
		}
		catch (CypherFailedException e) {
			throw new NonceRequestFailedException(e);
		}
	}

	public static void handleException(Exception e) {
		if (e instanceof StatusRuntimeException sre) {
			System.out.println(SERVER_ERROR_PREFIX + sre.getStatus().getDescription());
			System.out.println(OPERATION_FAILED);
		}
		else {
			System.out.println(ERROR_PREFIX + e.getMessage());
			System.out.println(OPERATION_FAILED);
		}
	}

	private ByteString getCypheredNonceToServer(long nonceToServer, int replicaID) throws CypherFailedException {
		return ByteString.copyFrom(signatureManager.cypherNonce(this.serverPublicKeys.get(replicaID), nonceToServer));
	}

	private int numberOfNeededResponses() {
		return (this.numberOfServerReplicas + this.faultsToTolerate) / 2;
	}

	public Transfer buildTransfer(long timestamp, PublicKey senderPublicKey, PublicKey receiverPublicKey, int amount) {
		Transfer.Builder builder = Transfer.newBuilder();
		builder.setTimestamp(timestamp);
		builder.setSenderKey(ByteString.copyFrom(senderPublicKey.getEncoded()));
		builder.setReceiverKey(ByteString.copyFrom(receiverPublicKey.getEncoded()));
		builder.setAmount(amount);
		return builder.build();
	}

	// Open Account
	public OpenAccountRequest buildOpenAccountRequest(long nonceToServer, int replicaID) throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		OpenAccountRequest.Builder builder = OpenAccountRequest.newBuilder();
		builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
		builder.setCypheredNonce(cypheredNonceToServer);
		OpenAccountRequest content = builder.build();
		debugRequestHistory.add(content);
		return content;
	}

	public SignedOpenAccountResponse openAccount(ServerServiceBlockingStub stub, OpenAccountRequest content, long nonceToClient)
			throws CypherFailedException {
		byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
		SignedOpenAccountRequest.Builder signedBuilder = SignedOpenAccountRequest.newBuilder();
		signedBuilder.setContent(content);
		signedBuilder.setSignature(ByteString.copyFrom(signature));
		SignedOpenAccountRequest signedRequest = signedBuilder.build();
		debugRequestHistory.add(signedRequest);
		return stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).openAccount(signedRequest);
	}

	public void openAccount() {
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		int numAcks = 0;
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				OpenAccountRequest request = buildOpenAccountRequest(nonceToServer, replicaID);
				var nonceToClient = requestNonce(stub);
				var response = openAccount(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				if (this.signatureManager.isSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature)) {
					numAcks++;
				}
			}
			catch (StatusRuntimeException | NonceRequestFailedException | CypherFailedException | SignatureVerificationFailedException e) {
				handleException(e);
			}
		}
		printNumAcks(numAcks);
	}

	public SendAmountRequest buildSendAmountRequest(long nonceToServer, int replicaID, Transfer transfer, byte[] senderSignature) throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		SendAmountRequest.Builder builder = SendAmountRequest.newBuilder();
		builder.setTransfer(transfer);
		builder.setSenderSignature(ByteString.copyFrom(senderSignature));
		builder.setCypheredNonce(cypheredNonceToServer);
		SendAmountRequest content = builder.build();
		debugRequestHistory.add(content);
		return content;
	}

	public SignedSendAmountResponse sendAmount(ServerServiceBlockingStub stub, SendAmountRequest content, long nonceToClient)
			throws CypherFailedException {
		byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
		SignedSendAmountRequest.Builder signedBuilder = SignedSendAmountRequest.newBuilder();
		signedBuilder.setContent(content);
		signedBuilder.setSignature(ByteString.copyFrom(signature));
		SignedSendAmountRequest signedRequest = signedBuilder.build();
		debugRequestHistory.add(signedRequest);
		return stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).sendAmount(signedRequest);
	}

	public void sendAmount(String destinationUserId, int amount)
			throws KeyPairLoadingFailedException, KeyPairGenerationFailedException, CypherFailedException {
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		Transfer newTransfer = buildTransfer(System.currentTimeMillis(), this.userPublicKey, Resources.getPublicKeyByUserId(destinationUserId), amount);
		byte[] senderSignature = this.signatureManager.sign(newTransfer.toByteArray());
		int numAcks = 0;
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				SendAmountRequest request = buildSendAmountRequest(nonceToServer, replicaID, newTransfer, senderSignature);
				var nonceToClient = requestNonce(stub);
				var response = sendAmount(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				if (this.signatureManager.isSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature)) {
					numAcks++;
				}
			}
			catch (StatusRuntimeException | NonceRequestFailedException | CypherFailedException | SignatureVerificationFailedException e) {
				handleException(e);
			}
		}
		printNumAcks(numAcks);
		if (numAcks > numberOfNeededResponses()) {
			// TODO: send "commit" to all replicas, which includes the list of nonces and list of responses
		}
	}

	private String buildTransferListString(List<Transfer> transferFieldsList)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < transferFieldsList.size(); i++) {
			Transfer transferFields = transferFieldsList.get(i);
			byte[] sourceKeyBytes = transferFields.getSenderKey().toByteArray();
			PublicKey sourceKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(sourceKeyBytes));
			String sourceKeyString = Base64.getEncoder().encodeToString(sourceKey.getEncoded());
			byte[] destinationKeyBytes = transferFields.getReceiverKey().toByteArray();
			PublicKey destinationKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(destinationKeyBytes));
			String destinationKeyString = Base64.getEncoder().encodeToString(destinationKey.getEncoded());
			int amount = transferFields.getAmount();
			String direction = (userPublicKey.equals(sourceKey)) ? "OUTGOING" : "INCOMING";
			builder.append("%s TRANSFER no.%d: %s $ %d from %s to %s%n".formatted(direction, i, amount, sourceKeyString, destinationKeyString));
		}
		return builder.toString();
	}

	public CheckAccountRequest buildCheckAccountRequest(long nonceToServer, int replicaID) throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		CheckAccountRequest.Builder builder = CheckAccountRequest.newBuilder();
		builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
		builder.setCypheredNonce(cypheredNonceToServer);
		CheckAccountRequest content = builder.build();
		debugRequestHistory.add(content);
		return content;
	}

	public SignedCheckAccountResponse checkAccount(ServerServiceBlockingStub stub, CheckAccountRequest content, long nonceToClient)
			throws CypherFailedException {
		byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
		SignedCheckAccountRequest.Builder signedBuilder = SignedCheckAccountRequest.newBuilder();
		signedBuilder.setContent(content);
		signedBuilder.setSignature(ByteString.copyFrom(signature));
		SignedCheckAccountRequest signedRequest = signedBuilder.build();
		debugRequestHistory.add(signedRequest);
		return stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).checkAccount(signedRequest);
	}

	public void checkAccount() throws NoSuchAlgorithmException, InvalidKeySpecException {
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		List<Long> noncesToClient = new ArrayList<>();
		List<SignedCheckAccountResponse> responses = new ArrayList<>();
		int numAcks = 0;
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				CheckAccountRequest request = buildCheckAccountRequest(nonceToServer, replicaID);
				var nonceToClient = requestNonce(stub);
				var response = checkAccount(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				byte[] responseContent = response.getContent().toByteArray();
				if (this.signatureManager.isSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature, responseContent)) {
					noncesToClient.add(nonceToClient);
					responses.add(response);
					numAcks++;
				}
			}
			catch (StatusRuntimeException | NonceRequestFailedException | CypherFailedException | SignatureVerificationFailedException e) {
				handleException(e);
			}
		}
		printNumAcks(numAcks);
		if (numAcks > numberOfNeededResponses()) {
			// TODO: send "commit" to all replicas, which includes the list of nonces and list of responses
			lastCheckAccountTransfers = responses.get(0).getContent().getPendingTransfersList();
			System.out.println("Balance: " + responses.get(0).getContent().getBalance());
			System.out.println("Pending Transfers: ");
			System.out.println(buildTransferListString(responses.get(0).getContent().getPendingTransfersList()));
		}
	}

	public Transfer getTransferFromNumber(int transferNum)
			throws InvalidTransferNumberException {
		if (lastCheckAccountTransfers == null) throw new InvalidTransferNumberException("No check account was performed, cannot select transfer");
		if (transferNum < 0 || transferNum >= lastCheckAccountTransfers.size()) throw new InvalidTransferNumberException();
		return lastCheckAccountTransfers.get(transferNum);
	}

	public ReceiveAmountRequest buildReceiveAmountRequest(long nonceToServer, int replicaID, Transfer transfer, byte[] receiverSignature)
			throws InvalidTransferNumberException, CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		ReceiveAmountRequest.Builder builder = ReceiveAmountRequest.newBuilder();
		builder.setTransfer(transfer);
		builder.setReceiverSignature(ByteString.copyFrom(receiverSignature));
		builder.setCypheredNonce(cypheredNonceToServer);
		ReceiveAmountRequest content = builder.build();
		debugRequestHistory.add(content);
		return content;
	}

	public SignedReceiveAmountResponse receiveAmount(ServerServiceBlockingStub stub, ReceiveAmountRequest content, long nonceToClient)
			throws CypherFailedException {
		byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
		SignedReceiveAmountRequest.Builder signedBuilder = SignedReceiveAmountRequest.newBuilder();
		signedBuilder.setContent(content);
		signedBuilder.setSignature(ByteString.copyFrom(signature));
		SignedReceiveAmountRequest signedRequest = signedBuilder.build();
		debugRequestHistory.add(signedRequest);
		return stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).receiveAmount(signedRequest);
	}

	public void receiveAmount(int transferNum) throws InvalidTransferNumberException, CypherFailedException {
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		Transfer targetTransfer = getTransferFromNumber(transferNum);
		byte[] receiverSignature = this.signatureManager.sign(targetTransfer.toByteArray());
		int numAcks = 0;
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				ReceiveAmountRequest request = buildReceiveAmountRequest(nonceToServer, replicaID, targetTransfer, receiverSignature);
				var nonceToClient = requestNonce(stub);
				var response = receiveAmount(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				if (this.signatureManager.isSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature)) {
					numAcks++;
				}
			}
			catch (StatusRuntimeException | NonceRequestFailedException | CypherFailedException | SignatureVerificationFailedException |
			       InvalidTransferNumberException e) {
				handleException(e);
			}
		}
		printNumAcks(numAcks);
		if (numAcks > numberOfNeededResponses()) {
			// TODO: send "commit" to all replicas, which includes the list of nonces and list of responses
		}
	}

	// Audit
	public AuditRequest buildAuditRequest(long nonceToServer, int replicaID) throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		AuditRequest.Builder builder = AuditRequest.newBuilder();
		builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
		builder.setCypheredNonce(cypheredNonceToServer);
		AuditRequest content = builder.build();
		debugRequestHistory.add(content);
		return content;
	}

	public SignedAuditResponse audit(ServerServiceBlockingStub stub, AuditRequest content, long nonceToClient)
			throws CypherFailedException {
		byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
		SignedAuditRequest.Builder signedBuilder = SignedAuditRequest.newBuilder();
		signedBuilder.setContent(content);
		signedBuilder.setSignature(ByteString.copyFrom(signature));
		SignedAuditRequest signedRequest = signedBuilder.build();
		debugRequestHistory.add(signedRequest);
		return stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).audit(signedRequest);
	}

	public void audit() throws NoSuchAlgorithmException, InvalidKeySpecException {
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		List<Long> noncesToClient = new ArrayList<>();
		List<SignedAuditResponse> responses = new ArrayList<>();
		int numAcks = 0;
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				AuditRequest request = buildAuditRequest(nonceToServer, replicaID);
				var nonceToClient = requestNonce(stub);
				var response = audit(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				byte[] responseContent = response.getContent().toByteArray();
				if (this.signatureManager.isSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature, responseContent)) {
					noncesToClient.add(nonceToClient);
					responses.add(response);
					numAcks++;
				}
			}
			catch (StatusRuntimeException | NonceRequestFailedException | CypherFailedException | SignatureVerificationFailedException e) {
				handleException(e);
			}
		}
		printNumAcks(numAcks);
		if (numAcks > numberOfNeededResponses()) {
			// TODO: send "commit" to all replicas, which includes the list of nonces and list of responses
			System.out.println("Transaction History: ");
			System.out.println(buildTransferListString(responses.get(0).getContent().getHistoryList()));
		}
	}
}