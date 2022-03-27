package pt.tecnico.sec.bftb.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sec.bftb.client.exceptions.*;
import pt.tecnico.sec.bftb.server.grpc.Server.*;
import pt.tecnico.sec.bftb.server.grpc.ServerServiceGrpc;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

public class Client {

	public static final String SERVER_ERROR_PREFIX = "SERVER ERROR: ";
	public static final String ERROR_PREFIX = "ERROR: ";
	public static final String OPERATION_SUCCESSFUL = "Operation successful!";
	public static final String OPERATION_FAILED = "Operation failed!";
	private static final long DEADLINE_SEC = 10;    // Timeout deadline in seconds
	private final ServerServiceGrpc.ServerServiceBlockingStub stub;
	private final SignatureManager signatureManager;
	private final PublicKey serverPublicKey;
	private PublicKey userPublicKey;
	private PrivateKey userPrivateKey;

	public Client(PublicKey userPublicKey, PrivateKey userPrivateKey, String serverURI) throws CertificateException {
		ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
		this.stub = ServerServiceGrpc.newBlockingStub(channel);
		this.signatureManager = new SignatureManager(userPrivateKey);
		this.userPublicKey = userPublicKey;
		this.userPrivateKey = userPrivateKey;
		this.serverPublicKey = Resources.getServerPublicKey();
	}

	public Client(String serverURI)
			throws CertificateException, KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		// Default user ID is "user", just for simplicity
		this(Resources.getPublicKeyByUserId("user"), Resources.getPrivateKeyByUserId("user"), serverURI);
	}

	public void changeUser(String userId) throws KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		userPrivateKey = Resources.getPrivateKeyByUserId(userId);
		userPublicKey = Resources.getPublicKeyByUserId(userId);
		signatureManager.setPrivateKey(userPrivateKey);
		System.out.printf("User changed to '%s'%n", userId);
	}

	private long requestNonce() throws NonceRequestFailedException {
		try {
			GetNonceRequest request = GetNonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(userPublicKey.getEncoded())).build();
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

	// Requests sign of life from the server
	public void ping(String input) {
		try {
			PingRequest.Builder builder = PingRequest.newBuilder();
			builder.setInput(input);
			String output = this.stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).ping(builder.build()).getOutput();
			System.out.println(output);
		}
		catch (StatusRuntimeException e) {
			System.out.println(SERVER_ERROR_PREFIX + e.getStatus().getDescription());
			System.out.println(OPERATION_FAILED);
		}
	}

	// Open Account
	public void openAccount() {
		try {
			long nonceToClient = requestNonce();
			OpenAccountRequest.Builder builder = OpenAccountRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			byte[] nonceToServer = signatureManager.generateCypheredNonce(this.serverPublicKey);
			builder.setCypheredNonce(ByteString.copyFrom(nonceToServer));
			OpenAccountRequest content = builder.build();
			byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
			SignedOpenAccountRequest.Builder signedBuilder = SignedOpenAccountRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			SignedOpenAccountResponse response = this.stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).openAccount(signedBuilder.build());
			byte[] serverSignature = response.getSignature().toByteArray();
			if (this.signatureManager.isSignatureInvalid(serverPublicKey, serverSignature)) {
				System.out.println(OPERATION_FAILED);
				return;
			}
			System.out.println(OPERATION_SUCCESSFUL);
		}
		catch (StatusRuntimeException e) {
			System.out.println(SERVER_ERROR_PREFIX + e.getStatus().getDescription());
			System.out.println(OPERATION_FAILED);
		}
		catch (CypherFailedException | NonceRequestFailedException | SignatureVerificationFailedException e) {
			System.out.println(ERROR_PREFIX + e.getMessage());
			System.out.println(OPERATION_FAILED);
		}
	}

	public void sendAmount(String destUserId, int amount) {
		try {
			PublicKey destinationPublicKey = Resources.getPublicKeyByUserId(destUserId);
			long nonceToClient = requestNonce();
			SendAmountRequest.Builder builder = SendAmountRequest.newBuilder();
			builder.setSourceKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			builder.setDestinationKey(ByteString.copyFrom(destinationPublicKey.getEncoded()));
			builder.setAmount(amount);
			byte[] nonceToServer = signatureManager.generateCypheredNonce(this.serverPublicKey);
			builder.setCypheredNonce(ByteString.copyFrom(nonceToServer));
			SendAmountRequest content = builder.build();
			byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
			SignedSendAmountRequest.Builder signedBuilder = SignedSendAmountRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			SignedSendAmountResponse response = this.stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).sendAmount(signedBuilder.build());
			byte[] serverSignature = response.getSignature().toByteArray();
			if (this.signatureManager.isSignatureInvalid(serverPublicKey, serverSignature)) {
				System.out.println(OPERATION_FAILED);
				return;
			}
			System.out.println(OPERATION_SUCCESSFUL);
		}
		catch (StatusRuntimeException e) {
			System.out.println(SERVER_ERROR_PREFIX + e.getStatus().getDescription());
			System.out.println(OPERATION_FAILED);
		}
		catch (CypherFailedException | NonceRequestFailedException | SignatureVerificationFailedException | KeyPairLoadingFailedException | KeyPairGenerationFailedException e) {
			System.out.println(ERROR_PREFIX + e.getMessage());
			System.out.println(OPERATION_FAILED);
		}
	}

	public void checkAccount() {
		try {
			long nonceToClient = requestNonce();
			CheckAccountRequest.Builder builder = CheckAccountRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			byte[] nonceToServer = signatureManager.generateCypheredNonce(this.serverPublicKey);
			builder.setCypheredNonce(ByteString.copyFrom(nonceToServer));
			CheckAccountRequest content = builder.build();
			byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
			SignedCheckAccountRequest.Builder signedBuilder = SignedCheckAccountRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			SignedCheckAccountResponse response = this.stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).checkAccount(signedBuilder.build());
			byte[] serverSignature = response.getSignature().toByteArray();
			if (this.signatureManager.isSignatureInvalid(serverPublicKey, serverSignature, response.getContent().toByteArray())) {
				System.out.println(OPERATION_FAILED);
				return;
			}
			System.out.println(OPERATION_SUCCESSFUL);
			System.out.println("Balance: " + response.getContent().getBalance());
			System.out.println("Pending Transfers: ");
			System.out.println(response.getContent().getPendingTransfers());
		}
		catch (StatusRuntimeException e) {
			System.out.println(SERVER_ERROR_PREFIX + e.getStatus().getDescription());
			System.out.println(OPERATION_FAILED);
		}
		catch (CypherFailedException | NonceRequestFailedException | SignatureVerificationFailedException e) {
			System.out.println(ERROR_PREFIX + e.getMessage());
			System.out.println(OPERATION_FAILED);
		}
	}

	public void receiveAmount(int transferNum) {
		try {
			long nonceToClient = requestNonce();
			ReceiveAmountRequest.Builder builder = ReceiveAmountRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			builder.setTransferNum(transferNum);
			byte[] nonceToServer = signatureManager.generateCypheredNonce(this.serverPublicKey);
			builder.setCypheredNonce(ByteString.copyFrom(nonceToServer));
			ReceiveAmountRequest content = builder.build();
			byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
			SignedReceiveAmountRequest.Builder signedBuilder = SignedReceiveAmountRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			SignedReceiveAmountResponse response = this.stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).receiveAmount(signedBuilder.build());
			byte[] serverSignature = response.getSignature().toByteArray();
			if (this.signatureManager.isSignatureInvalid(serverPublicKey, serverSignature)) {
				System.out.println(OPERATION_FAILED);
				return;
			}
			System.out.println(OPERATION_SUCCESSFUL);
		}
		catch (StatusRuntimeException e) {
			System.out.println(SERVER_ERROR_PREFIX + e.getStatus().getDescription());
		}
		catch (CypherFailedException | NonceRequestFailedException | SignatureVerificationFailedException e) {
			System.out.println(ERROR_PREFIX + e.getMessage());
		}
	}

	public void audit() {
		try {
			long nonceToClient = requestNonce();
			AuditRequest.Builder builder = AuditRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			byte[] nonceToServer = signatureManager.generateCypheredNonce(this.serverPublicKey);
			builder.setCypheredNonce(ByteString.copyFrom(nonceToServer));
			AuditRequest content = builder.build();
			byte[] signature = this.signatureManager.sign(nonceToClient, content.toByteArray());
			SignedAuditRequest.Builder signedBuilder = SignedAuditRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			SignedAuditResponse response = this.stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).audit(signedBuilder.build());
			byte[] serverSignature = response.getSignature().toByteArray();
			if (this.signatureManager.isSignatureInvalid(serverPublicKey, serverSignature, response.getContent().toByteArray())) {
				System.out.println(OPERATION_FAILED);
				return;
			}
			System.out.println(OPERATION_SUCCESSFUL);
			System.out.println("Transaction History: ");
			System.out.println(response.getContent().getHistory());
		}
		catch (StatusRuntimeException e) {
			System.out.println(SERVER_ERROR_PREFIX + e.getStatus().getDescription());
			System.out.println(OPERATION_FAILED);
		}
		catch (CypherFailedException | NonceRequestFailedException | SignatureVerificationFailedException e) {
			System.out.println(ERROR_PREFIX + e.getMessage());
			System.out.println(OPERATION_FAILED);
		}
	}

}