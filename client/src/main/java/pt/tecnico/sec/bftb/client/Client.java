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

import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Client {

	public static final String SERVER_ERROR_PREFIX = "SERVER ERROR: ";
	public static final String ERROR_PREFIX = "ERROR: ";
	public static final String OPERATION_SUCCESSFUL = "Operation successful!";
	public static final String OPERATION_FAILED = "Operation failed!";
	private static final String FULL_OPERATION_SUCCESSFUL = "Full Operation successful!";
	private static final long DEADLINE_SEC = 10;    // Timeout deadline in seconds
	private static final int INITIAL_BALANCE = 100; // Initial balance of an account (must be the same as in the server)
	private static final long PUZZLE_SEARCH_RANGE = 100000;
	private final ConcurrentHashMap<Integer, ServerServiceBlockingStub> stubs;
	private final SignatureManager signatureManager;
	private final Map<Integer, PublicKey> serverPublicKeys;
	private PublicKey userPublicKey;
	private PrivateKey userPrivateKey;
	private List<Transfer> lastCheckAccountTransfers = null;
	private final int faultsToTolerate;
	private final int numberOfServerReplicas;
	private Puzzle currentPuzzle = null;
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
		this.signatureManager = new SignatureManager(this.userPrivateKey, this.userPublicKey);
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
		signatureManager.setKeyPair(Resources.getPrivateKeyByUserId(userId), Resources.getPublicKeyByUserId(userId));
	}

	public void changeUser(String userId) throws KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		userPrivateKey = Resources.getPrivateKeyByUserId(userId);
		userPublicKey = Resources.getPublicKeyByUserId(userId);
		signatureManager.setKeyPair(userPrivateKey, userPublicKey);
		lastCheckAccountTransfers = null;
		System.out.printf("User changed to '%s'%n", userId);
	}

	public long requestNonce(ServerServiceBlockingStub stub) throws NonceRequestFailedException {
		try {
			GetNonceRequest request = GetNonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(userPublicKey.getEncoded())).build();
			debugRequestHistory.add(request);
			GetNonceResponse response = stub.getNonce(request);
			byte[] cypheredNonce = response.getCypheredNonce().toByteArray();
			currentPuzzle = response.getPuzzle();
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
		else if (e instanceof NotEnoughValidResponsesException nevre) {
			System.out.println("Overall Operation Failed: " + nevre.getMessage());
		}
		else {
			System.out.println(ERROR_PREFIX + e.getMessage());
			System.out.println(OPERATION_FAILED);
		}
	}

	public long findPuzzleSolution()
			throws NoCurrentPuzzleException, NoSuchAlgorithmException, NoPuzzleSolutionFoundException {
		if (currentPuzzle == null) throw new NoCurrentPuzzleException();
		byte[] puzzle = currentPuzzle.getPuzzle().toByteArray();
		byte[] salt = currentPuzzle.getPuzzleSalt().toByteArray();
		for (long i = 0; i < PUZZLE_SEARCH_RANGE; i++) {
			byte[] rawPuzzle = ByteBuffer.allocate(Long.BYTES + salt.length).putLong(i).put(salt).array();
			byte[] attempt = MessageDigest.getInstance("SHA-256").digest(rawPuzzle);
			if (Arrays.equals(puzzle, attempt)) return i;
		}
		throw new NoPuzzleSolutionFoundException();
	}

	private PublicKey publicKeyFromByteString(ByteString publicKeyBS) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBS.toByteArray()));
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

	public ListSizes buildListSizes(int pendingSize, int approvedSize, int wts) {
		ListSizes.Builder builder = ListSizes.newBuilder();
		builder.setPendingSize(pendingSize);
		builder.setApprovedSize(approvedSize);
		builder.setWts(wts);
		return builder.build();
	}

	// Open Account
	public OpenAccountRequest buildOpenAccountRequest(long nonceToServer, int replicaID, Balance initialBalance, byte[] balanceSignature, ListSizes listSizes, byte[] sizesSignature) throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		OpenAccountRequest.Builder builder = OpenAccountRequest.newBuilder();
		builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
		builder.setCypheredNonce(cypheredNonceToServer);
		builder.setBalance(initialBalance);
		builder.setBalanceSignature(ByteString.copyFrom(balanceSignature));
		builder.setListSizes(listSizes);
		builder.setListSizesSignature(ByteString.copyFrom(sizesSignature));
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

	public void openAccount() throws CypherFailedException {
		Balance initialBalance = buildInitialBalance();
		byte[] balanceSignature = this.signatureManager.signBalance(initialBalance);
		ListSizes listSizes = buildListSizes(0, 0, 0);
		byte[] sizesSignature = this.signatureManager.signListSizes(listSizes);
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		int numAcks = 0;
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				var nonceToClient = requestNonce(stub);
				OpenAccountRequest request = buildOpenAccountRequest(nonceToServer, replicaID, initialBalance, balanceSignature, listSizes, sizesSignature);
				var response = openAccount(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				if (this.signatureManager.isNonceSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature)) {
					numAcks++;
				}
			}
			catch (StatusRuntimeException | NonceRequestFailedException | CypherFailedException | SignatureVerificationFailedException e) {
				handleException(e);
			}
		}
		printNumAcks(numAcks);
	}

	private Balance buildInitialBalance() {
		Balance.Builder builder = Balance.newBuilder();
		builder.setValue(INITIAL_BALANCE);
		builder.setWts(0);
		return builder.build();
	}

	public ReadForWriteRequest buildReadForWriteRequest(long nonceToServer, int replicaID, ByteString senderKey, ByteString receiverKey, boolean isSender) throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		ReadForWriteRequest.Builder builder = ReadForWriteRequest.newBuilder();
		builder.setSenderKey(senderKey);
		builder.setReceiverKey(receiverKey);
		builder.setIsSender(isSender);
		builder.setCypheredNonce(cypheredNonceToServer);
		ReadForWriteRequest content = builder.build();
		debugRequestHistory.add(content);
		return content;
	}

	public SignedReadForWriteResponse readForWrite(ServerServiceBlockingStub stub, ReadForWriteRequest content, long nonceToClient)
			throws CypherFailedException {
		byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
		SignedReadForWriteRequest.Builder signedBuilder = SignedReadForWriteRequest.newBuilder();
		signedBuilder.setContent(content);
		signedBuilder.setSignature(ByteString.copyFrom(signature));
		SignedReadForWriteRequest signedRequest = signedBuilder.build();
		debugRequestHistory.add(signedRequest);
		return stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).readForWrite(signedRequest);
	}

	public InfoForWrite readForWrite(ByteString senderKey, ByteString receiverKey, boolean isSender) throws NotEnoughValidResponsesException {
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		List<ReadForWriteResponse> readList = new ArrayList<>();
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				var nonceToClient = requestNonce(stub);
				ReadForWriteRequest request = buildReadForWriteRequest(nonceToServer, replicaID, senderKey, receiverKey, isSender);
				var response = readForWrite(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				var content = response.getContent();
				if (!this.signatureManager.isNonceSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature, content.toByteArray()))
					throw new InvalidSignatureException("Invalid nonce signature");
				if (!this.signatureManager.isBalanceSignatureValid(content.getBalanceSignature().toByteArray(), content.getBalance()))
					throw new InvalidSignatureException("Invalid balance signature");
				if (!this.signatureManager.isListSizesSignatureValid(publicKeyFromByteString(content.getReceiverListSizesSigner()), content.getReceiverListSizesSignature().toByteArray(), content.getReceiverListSizes()))
					throw new InvalidSignatureException("Invalid receiver list sizes signature");
				if (!this.signatureManager.isListSizesSignatureValid(publicKeyFromByteString(content.getSenderListSizesSigner()), content.getSenderListSizesSignature().toByteArray(), content.getSenderListSizes()))
					throw new InvalidSignatureException("Invalid sender list sizes signature");
				readList.add(content);
			}
			catch (Exception e) {
				handleException(e);
			}
		}
		printNumAcks(readList.size());
		if (readList.size() >= numberOfNeededResponses()) {
			return new InfoForWrite(getMostRecentBalance(readList), getMostRecentListSizes(readList, 0), getMostRecentListSizes(readList, 1));
		}
		else {
			throw new NotEnoughValidResponsesException();
		}
	}

	private Balance getMostRecentBalance(List<ReadForWriteResponse> readList) throws NotEnoughValidResponsesException {
		int highestWts = -1;
		Balance mostRecentBalance = null;
		for (ReadForWriteResponse response : readList) {
			Balance balance = response.getBalance();
			if (balance.getWts() > highestWts) {
				highestWts = balance.getWts();
				mostRecentBalance = balance;
			}
		}
		if (mostRecentBalance == null) throw new NotEnoughValidResponsesException();
		return mostRecentBalance;
	}

	private ListSizes getMostRecentListSizes(List<ReadForWriteResponse> readList, int mode) throws NotEnoughValidResponsesException {
		int highestWts = -1;
		ListSizes mostRecentListSizes = null;
		for (ReadForWriteResponse response : readList) {
			ListSizes listSizes = (mode == 0) ? response.getSenderListSizes() : response.getReceiverListSizes();
			if (listSizes.getWts() > highestWts) {
				highestWts = listSizes.getWts();
				mostRecentListSizes = listSizes;
			}
		}
		if (mostRecentListSizes == null) throw new NotEnoughValidResponsesException();
		return mostRecentListSizes;
	}

	public SendAmountRequest buildSendAmountRequest(long nonceToServer, int replicaID, Transfer transfer, byte[] senderSignature,
			Balance balance, byte[] balanceSignature, ListSizes receiverListSizes, byte[] receiverSizesSignature)
			throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		SendAmountRequest.Builder builder = SendAmountRequest.newBuilder();
		builder.setTransfer(transfer);
		builder.setSenderTransferSignature(ByteString.copyFrom(senderSignature));
		builder.setNewBalance(balance);
		builder.setBalanceSignature(ByteString.copyFrom(balanceSignature));
		builder.setReceiverListSizes(receiverListSizes);
		builder.setReceiverSizesSignature(ByteString.copyFrom(receiverSizesSignature));
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
			throws KeyPairLoadingFailedException, KeyPairGenerationFailedException, CypherFailedException,
			NotEnoughValidResponsesException {
		Transfer newTransfer = buildTransfer(System.currentTimeMillis(), this.userPublicKey, Resources.getPublicKeyByUserId(destinationUserId), amount);
		byte[] senderSignature = this.signatureManager.sign(newTransfer.toByteArray());
		InfoForWrite infoForWrite = readForWrite(ByteString.copyFrom(this.userPublicKey.getEncoded()), ByteString.copyFrom(Resources.getPublicKeyByUserId(destinationUserId).getEncoded()), true);
		Balance newBalance = getDecrementedBalance(infoForWrite.getBalance(), amount);
		byte[] balanceSignature = this.signatureManager.signBalance(newBalance);
		ListSizes newReceiverListSizes = getListSizesNewPending(infoForWrite.getSenderListSizes());
		byte[] receiverSizesSignature = this.signatureManager.signListSizes(newReceiverListSizes);
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		int numAcks = 0;
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				var nonceToClient = requestNonce(stub);
				SendAmountRequest request = buildSendAmountRequest(nonceToServer, replicaID, newTransfer, senderSignature,
						newBalance, balanceSignature, newReceiverListSizes, receiverSizesSignature);
				var response = sendAmount(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				if (!this.signatureManager.isNonceSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature))
					throw new InvalidSignatureException("Nonce signature is invalid");
				numAcks++;
			}
			catch (Exception e) {
				handleException(e);
			}
		}
		printNumAcks(numAcks);
		if (numAcks >= numberOfNeededResponses()) {
			System.out.println(FULL_OPERATION_SUCCESSFUL);
		}
		else {
			throw new NotEnoughValidResponsesException();
		}
	}

	private ListSizes getListSizesNewPending(ListSizes listSizes) {
		ListSizes.Builder builder = ListSizes.newBuilder();
		builder.setPendingSize(listSizes.getPendingSize() + 1);
		builder.setApprovedSize(listSizes.getApprovedSize());
		builder.setWts(listSizes.getWts() + 1);
		return builder.build();
	}

	private ListSizes getListSizesNewApproved(ListSizes listSizes) {
		ListSizes.Builder builder = ListSizes.newBuilder();
		builder.setPendingSize(listSizes.getPendingSize());
		builder.setApprovedSize(listSizes.getApprovedSize() + 1);
		builder.setWts(listSizes.getWts() + 1);
		return builder.build();
	}

	private ListSizes getListSizesPendingToApproved(ListSizes listSizes) {
		ListSizes.Builder builder = ListSizes.newBuilder();
		builder.setPendingSize(listSizes.getPendingSize() - 1);
		builder.setApprovedSize(listSizes.getApprovedSize() + 1);
		builder.setWts(listSizes.getWts() + 1);
		return builder.build();
	}

	private Balance getDecrementedBalance(Balance currentBalance, int amount) {
		return getIncrementedBalance(currentBalance, -amount);
	}

	private Balance getIncrementedBalance(Balance currentBalance, int amount) {
		Balance.Builder builder = Balance.newBuilder();
		builder.setValue(currentBalance.getValue() + amount);
		builder.setWts(currentBalance.getWts() + 1);
		return builder.build();
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
			builder.append("%s TRANSFER no.%d: $ %d from %s to %s%n".formatted(direction, i, amount, sourceKeyString, destinationKeyString));
		}
		return builder.toString();
	}

	public CheckAccountRequest buildCheckAccountRequest(long nonceToServer, int replicaID, long puzzleSolution) throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		CheckAccountRequest.Builder builder = CheckAccountRequest.newBuilder();
		builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
		builder.setCypheredNonce(cypheredNonceToServer);
		builder.setPuzzleSolution(puzzleSolution);
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

	public void checkAccount()
			throws NoSuchAlgorithmException, InvalidKeySpecException, NotEnoughValidResponsesException {
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		List<CheckAccountResponse> readList = new ArrayList<>();
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				var nonceToClient = requestNonce(stub);
				CheckAccountRequest request = buildCheckAccountRequest(nonceToServer, replicaID, findPuzzleSolution());
				var response = checkAccount(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				var content = response.getContent();
				if (!this.signatureManager.isNonceSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature, content.toByteArray()))
					throw new InvalidSignatureException("Nonce signature is invalid");
				readList.add(content);
				System.out.println(OPERATION_SUCCESSFUL);
			}
			catch (Exception e) {
				handleException(e);
			}
		}
		printNumAcks(readList.size());
		if (readList.size() >= numberOfNeededResponses()) {
			int balance = getMostRecentBalanceValue(readList);
			List<Transfer> pendingTransfers = getMostRecentPendingTransfersList(readList);
			this.lastCheckAccountTransfers = pendingTransfers;
			System.out.println(FULL_OPERATION_SUCCESSFUL);
			System.out.println("Balance: " + balance);
			System.out.println("Pending Transfers: ");
			System.out.println(buildTransferListString(pendingTransfers));
		}
		else {
			throw new NotEnoughValidResponsesException();
		}
	}

	private int getMostRecentBalanceValue(List<CheckAccountResponse> readList) throws NotEnoughValidResponsesException {
		int highestWts = -1;
		CheckAccountResponse mostRecentBalance = null;
		for (CheckAccountResponse response : readList) {
			int wts = response.getBalance().getWts();
			if (wts > highestWts) {
				highestWts = wts;
				mostRecentBalance = response;
			}
		}
		if (mostRecentBalance == null) throw new NotEnoughValidResponsesException();
		return mostRecentBalance.getBalance().getValue();
	}

	private List<Transfer> getMostRecentPendingTransfersList(List<CheckAccountResponse> readList) throws NotEnoughValidResponsesException {
		int highestWts = -1;
		CheckAccountResponse bestResponse = null;
		for (CheckAccountResponse response : readList) {
			int wts = response.getListSizes().getWts();
			if (wts > highestWts) {
				highestWts = wts;
				bestResponse = response;
			}
		}
		if (bestResponse == null) throw new NotEnoughValidResponsesException();
		return bestResponse.getPendingTransfersList();
	}

	public Transfer getTransferFromNumber(int transferNum)
			throws InvalidTransferNumberException {
		if (lastCheckAccountTransfers == null) throw new InvalidTransferNumberException("No check account was performed, cannot select transfer");
		if (transferNum < 0 || transferNum >= lastCheckAccountTransfers.size()) throw new InvalidTransferNumberException();
		return lastCheckAccountTransfers.get(transferNum);
	}

	public ReceiveAmountRequest buildReceiveAmountRequest(long nonceToServer, int replicaID, Transfer transfer,
			byte[] receiverSignature, Balance balance, byte[] balanceSignature, ListSizes senderListSizes,
			byte[] senderSizesSignature, ListSizes receiverListSizes, byte[] receiverSizesSignature)
			throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		ReceiveAmountRequest.Builder builder = ReceiveAmountRequest.newBuilder();
		builder.setTransfer(transfer);
		builder.setReceiverTransferSignature(ByteString.copyFrom(receiverSignature));
		builder.setNewBalance(balance);
		builder.setBalanceSignature(ByteString.copyFrom(balanceSignature));
		builder.setSenderListSizes(senderListSizes);
		builder.setSenderSizesSignature(ByteString.copyFrom(senderSizesSignature));
		builder.setReceiverListSizes(receiverListSizes);
		builder.setReceiverSizesSignature(ByteString.copyFrom(receiverSizesSignature));
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

	public void receiveAmount(int transferNum)
			throws InvalidTransferNumberException, CypherFailedException, NotEnoughValidResponsesException {
		Transfer targetTransfer = getTransferFromNumber(transferNum);
		byte[] receiverSignature = this.signatureManager.sign(targetTransfer.toByteArray());
		InfoForWrite infoForWrite = readForWrite(targetTransfer.getSenderKey(), targetTransfer.getReceiverKey(), false);
		Balance newBalance = getIncrementedBalance(infoForWrite.getBalance(), targetTransfer.getAmount());
		byte[] balanceSignature = this.signatureManager.signBalance(newBalance);
		ListSizes senderListSizes = getListSizesNewApproved(infoForWrite.getSenderListSizes());
		byte[] senderSizesSignature = this.signatureManager.signListSizes(senderListSizes);
		ListSizes receiverListSizes = getListSizesPendingToApproved(infoForWrite.getReceiverListSizes());
		byte[] receiverSizesSignature = this.signatureManager.signListSizes(receiverListSizes);
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		int numAcks = 0;
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				var nonceToClient = requestNonce(stub);
				ReceiveAmountRequest request = buildReceiveAmountRequest(nonceToServer, replicaID, targetTransfer, receiverSignature,
						newBalance, balanceSignature, senderListSizes, senderSizesSignature, receiverListSizes, receiverSizesSignature);
				var response = receiveAmount(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				if (!this.signatureManager.isNonceSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature))
					throw new InvalidSignatureException("Nonce signature is invalid");
				numAcks++;
				System.out.println(OPERATION_SUCCESSFUL);
			}
			catch (Exception e) {
				handleException(e);
			}
		}
		printNumAcks(numAcks);
		if (numAcks >= numberOfNeededResponses()) {
			System.out.println(FULL_OPERATION_SUCCESSFUL);
		}
		else {
			throw new NotEnoughValidResponsesException();
		}

	}

	// Audit
	public AuditRequest buildAuditRequest(long nonceToServer, int replicaID, long puzzleSolution) throws CypherFailedException {
		ByteString cypheredNonceToServer = getCypheredNonceToServer(nonceToServer, replicaID);
		AuditRequest.Builder builder = AuditRequest.newBuilder();
		builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
		builder.setCypheredNonce(cypheredNonceToServer);
		builder.setPuzzleSolution(puzzleSolution);
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

	public void audit() throws NoSuchAlgorithmException, InvalidKeySpecException, NotEnoughValidResponsesException {
		long nonceToServer = this.signatureManager.generateNonce(); // We use the same nonce for all server replicas
		List<AuditResponse> readList = new ArrayList<>();
		for (int replicaID = 0; replicaID < numberOfServerReplicas; replicaID++) {
			ServerServiceBlockingStub stub = stubs.get(replicaID);
			try {
				var nonceToClient = requestNonce(stub);
				AuditRequest request = buildAuditRequest(nonceToServer, replicaID, findPuzzleSolution());
				var response = audit(stub, request, nonceToClient);
				byte[] serverSignature = response.getSignature().toByteArray();
				var content = response.getContent();
				if (!this.signatureManager.isNonceSignatureValid(this.serverPublicKeys.get(replicaID), serverSignature, content.toByteArray()))
					throw new InvalidSignatureException("Nonce signature is invalid");
				if (!this.signatureManager.isListSizesSignatureValid(publicKeyFromByteString(content.getListSizesSigner()), content.getListSizesSignature().toByteArray(), content.getListSizes()))
					throw new InvalidSignatureException("List sizes signature is invalid");
				if (!isApprovedTransferListValid(content.getApprovedTransfersList(), content.getSenderTransferSignaturesList(), content.getReceiverTransferSignaturesList(), content.getListSizes().getApprovedSize()))
					throw new InvalidSignatureException("Approved transfer list has invalid signatures");
				readList.add(content);
				System.out.println(OPERATION_SUCCESSFUL);
			}
			catch (Exception e) {
				handleException(e);
			}
		}
		printNumAcks(readList.size());
		if (readList.size() >= numberOfNeededResponses()) {
			var approvedTransfers = getMostRecentApprovedTransfersList(readList);
			System.out.println(FULL_OPERATION_SUCCESSFUL);
			System.out.println("Transaction History: ");
			System.out.println(buildTransferListString(approvedTransfers));
		}
		else {
			throw new NotEnoughValidResponsesException();
		}
	}

	private List<Transfer> getMostRecentApprovedTransfersList(List<AuditResponse> readList) throws NotEnoughValidResponsesException {
		int highestWts = -1;
		AuditResponse bestResponse = null;
		for (AuditResponse response : readList) {
			int wts = response.getListSizes().getWts();
			if (wts > highestWts) {
				highestWts = wts;
				bestResponse = response;
			}
		}
		if (bestResponse == null) throw new NotEnoughValidResponsesException();
		return bestResponse.getApprovedTransfersList();
	}

	public boolean isPendingTransferListValid(List<Transfer> transfers, List<ByteString> senderSignatures, int expectedSize)
			throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureVerificationFailedException {
		if (transfers.size() != expectedSize) return false;
		if (senderSignatures.size() != expectedSize) return false;

		Set<byte[]> transferHashes = new HashSet<>();
		for (int i = 0; i < expectedSize; i++) {
			Transfer transfer = transfers.get(i);
			byte[] senderSignature = senderSignatures.get(i).toByteArray();
			if (!this.signatureManager.isTransferSignatureValid(publicKeyFromByteString(transfer.getSenderKey()), senderSignature, transfer)) return false;
			byte[] transferHash = getTransferHash(transfer);
			if (transferHashes.contains(transferHash)) return false;
			transferHashes.add(transferHash);
		}
		return true;
	}

	public boolean isApprovedTransferListValid(List<Transfer> transfers, List<ByteString> senderSignatures, List<ByteString> receiverSignatures, int expectedSize)
			throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureVerificationFailedException {
		if (transfers.size() != expectedSize) return false;
		if (senderSignatures.size() != expectedSize) return false;
		if (receiverSignatures.size() != expectedSize) return false;

		Set<byte[]> transferHashes = new HashSet<>();
		for (int i = 0; i < expectedSize; i++) {
			Transfer transfer = transfers.get(i);
			byte[] senderSignature = senderSignatures.get(i).toByteArray();
			byte[] receiverSignature = receiverSignatures.get(i).toByteArray();
			if (!this.signatureManager.isTransferSignatureValid(publicKeyFromByteString(transfer.getSenderKey()), senderSignature, transfer)) return false;
			if (!this.signatureManager.isTransferSignatureValid(publicKeyFromByteString(transfer.getReceiverKey()), receiverSignature, transfer)) return false;
			byte[] transferHash = getTransferHash(transfer);
			if (transferHashes.contains(transferHash)) return false;
			transferHashes.add(transferHash);
		}
		return true;
	}

	private byte[] getTransferHash(Transfer transfer) throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-256").digest(transfer.toByteArray());
	}
}