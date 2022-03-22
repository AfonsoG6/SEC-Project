package pt.tecnico.sec.bftb.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sec.bftb.server.grpc.Server.*;
import pt.tecnico.sec.bftb.server.grpc.ServerServiceGrpc;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

public class Client {

	private static final long DEADLINE_SEC = 10;    // Timeout deadline in seconds
	public static final String ASK_FOR_HELP = "Use the command 'help' to see the available commands.";
	public static final String ERROR_NUMBER_OF_ARGUMENTS = "ERROR: Invalid number of arguments! "+ASK_FOR_HELP;
	public static final String UNKNOWN_COMMAND = "ERROR: Unknown command! "+ASK_FOR_HELP;
	public static final String HELP_STRING = "Available Commands:%n" +
			"- ping <one word>                Check if server is responsive%n" +
			"- user <username>                Change to another user%n" +
			"- open                           Open a new account%n" +
			"- send <destination> <amount>    Send to the destination account the specified amount%n" +
			"- receive                        Confirm the earliest pending incoming transfer%n" +
			"- receive <transfer number>      Confirm the pending incoming transfer with the specified number%n" +
			"- check                          Obtain the balance of the account, and the list of pending transfers%n" +
			"- audit                          Obtain the full transaction history of the account%n" +
			"- exit                           Exit the App%n";

	private PublicKey userPublicKey;
	private PrivateKey userPrivateKey;
	private final String serverURI;

	public Client(PublicKey userPublicKey, PrivateKey userPrivateKey, String serverURI) {
		this.userPublicKey = userPublicKey;
		this.userPrivateKey = userPrivateKey;
		this.serverURI = serverURI;
	}

	public Client(String serverURI) {
		// Default user ID is "user", just for simplicity
		this(Resources.getPublicKeyByUserId("user"), Resources.getPrivateKeyByUserId("user"), serverURI);
	}

	// Returns true if the App should exit
	public boolean parseAndExecCommand(String line) {
		try {
			String[] tokens = line.split(" ");
			switch (tokens[0]) {
				case "#":
					// ignores line
					break;
				case "exit":
					return true;
				case "help":
					System.out.printf(HELP_STRING);
					break;
				case "ping":
					if (tokens.length == 2) ping(tokens[1]);
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "user":
					if (tokens.length == 2) changeUser(tokens[1]);
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "open":
					if (tokens.length == 1) openAccount();
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "send":
					if (tokens.length == 3) sendAmount(Resources.getPublicKeyByUserId(tokens[1]), Integer.parseInt(tokens[2]));
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "check":
					if (tokens.length == 1) checkAccount();
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "receive":
					if (tokens.length == 2) receiveAmount(0);
					else if (tokens.length == 3) receiveAmount(Integer.parseInt(tokens[2]));
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "audit":
					if (tokens.length == 1) audit();
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				default:
					System.out.println(UNKNOWN_COMMAND);
					break;
			}
		}
		catch (NumberFormatException| PatternSyntaxException e) {
			System.out.println(UNKNOWN_COMMAND);
		}
		return false;
	}

	private void changeUser(String userId) {
		userPrivateKey = Resources.getPrivateKeyByUserId(userId);
		userPublicKey = Resources.getPublicKeyByUserId(userId);
		System.out.printf("User changed to '%s'%n", userId);
	}

	private long requestNonce() {
		try {
			ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
			ServerServiceGrpc.ServerServiceBlockingStub stub = ServerServiceGrpc.newBlockingStub(channel);
			GetNonceRequest request = GetNonceRequest.newBuilder().setPublicKey(ByteString.copyFrom(userPublicKey.getEncoded())).build();
			GetNonceResponse response = stub.getNonce(request);
			channel.shutdown();
			byte[] cypheredNonce = response.getCypheredNonce().toByteArray();
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, userPrivateKey);
			byte[] nonceBytes = cipher.doFinal(cypheredNonce);
			channel.shutdown();
			return ByteBuffer.wrap(nonceBytes).getLong();
		}
		catch (StatusRuntimeException | IllegalBlockSizeException | BadPaddingException |
				InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}


	// TODO: cipher message together with nonce
	private byte[] getSignature(long nonce) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, userPrivateKey);
			byte[] nonceBytes = ByteBuffer.allocate(Long.BYTES).putLong(nonce).array();
			return cipher.doFinal(nonceBytes);
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	// Requests sign of life from the server
	public void ping(String input) {
		ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
		try {
			PingRequest.Builder builder = PingRequest.newBuilder();
			builder.setInput(input);
			ServerServiceGrpc.ServerServiceBlockingStub stub = ServerServiceGrpc.newBlockingStub(channel);
			String output = stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).ping(builder.build()).getOutput();
			System.out.println(output);
		}
		catch (StatusRuntimeException e) {
			System.out.println("ERROR: " + e.getStatus().getDescription());
		}
		channel.shutdown();
	}

	// Open Account
	public void openAccount() {
		ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
		try {
			long nonce = requestNonce();
			byte[] signature = getSignature(nonce);
			OpenAccountRequest.Builder builder = OpenAccountRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			OpenAccountRequest content = builder.build();
			SignedOpenAccountRequest.Builder signedBuilder = SignedOpenAccountRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			ServerServiceGrpc.ServerServiceBlockingStub stub = ServerServiceGrpc.newBlockingStub(channel);
			stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).openAccount(signedBuilder.build());
		}
		catch (StatusRuntimeException e) {
			System.out.println("ERROR: " + e.getStatus().getDescription());
		}
		channel.shutdown();
	}

	public void sendAmount(PublicKey destinationPublicKey, int amount) {
		ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
		try {
			long nonce = requestNonce();
			byte[] signature = getSignature(nonce);
			SendAmountRequest.Builder builder = SendAmountRequest.newBuilder();
			builder.setSourceKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			builder.setDestinationKey(ByteString.copyFrom(destinationPublicKey.getEncoded()));
			builder.setAmount(amount);
			SendAmountRequest content = builder.build();
			SignedSendAmountRequest.Builder signedBuilder = SignedSendAmountRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			ServerServiceGrpc.ServerServiceBlockingStub stub = ServerServiceGrpc.newBlockingStub(channel);
			stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).sendAmount(signedBuilder.build());
		}
		catch (StatusRuntimeException e) {
			System.out.println("ERROR: " + e.getStatus().getDescription());
		}
		channel.shutdown();
	}

	public void checkAccount() {
		ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
		try {
			long nonce = requestNonce();
			byte[] signature = getSignature(nonce);
			CheckAccountRequest.Builder builder = CheckAccountRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			CheckAccountRequest content = builder.build();
			SignedCheckAccountRequest.Builder signedBuilder = SignedCheckAccountRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			ServerServiceGrpc.ServerServiceBlockingStub stub = ServerServiceGrpc.newBlockingStub(channel);
			CheckAccountResponse response = stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).checkAccount(signedBuilder.build());
			System.out.println("Balance: " + response.getBalance());
			System.out.println("Pending Transfers: ");
			System.out.println(response.getPendingTransfers());
		}
		catch (StatusRuntimeException e) {
			System.out.println("ERROR: " + e.getStatus().getDescription());
		}
		channel.shutdown();
	}

	public void receiveAmount(int transferNum) {
		try {
			long nonce = requestNonce();
			byte[] signature = getSignature(nonce);
			ReceiveAmountRequest.Builder builder = ReceiveAmountRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			builder.setTransferNum(transferNum);
			ReceiveAmountRequest content = builder.build();
			SignedReceiveAmountRequest.Builder signedBuilder = SignedReceiveAmountRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
			ServerServiceGrpc.ServerServiceBlockingStub stub = ServerServiceGrpc.newBlockingStub(channel);
			stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).receiveAmount(signedBuilder.build());
		}
		catch (StatusRuntimeException e) {
			System.out.println("ERROR: " + e.getStatus().getDescription());
		}
	}

	public void audit() {
		try {
			long nonce = requestNonce();
			byte[] signature = getSignature(nonce);
			AuditRequest.Builder builder = AuditRequest.newBuilder();
			builder.setPublicKey(ByteString.copyFrom(this.userPublicKey.getEncoded()));
			AuditRequest content = builder.build();
			SignedAuditRequest.Builder signedBuilder = SignedAuditRequest.newBuilder();
			signedBuilder.setContent(content);
			signedBuilder.setSignature(ByteString.copyFrom(signature));
			ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
			ServerServiceGrpc.ServerServiceBlockingStub stub = ServerServiceGrpc.newBlockingStub(channel);
			AuditResponse response = stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).audit(signedBuilder.build());
			System.out.println("Transaction History: ");
			System.out.println(response.getHistory());
		}
		catch (StatusRuntimeException e) {
			System.out.println("ERROR: " + e.getStatus().getDescription());
		}
	}

}