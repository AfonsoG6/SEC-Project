package pt.tecnico.sec.bftb.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sec.bftb.server.grpc.Server.*;
import pt.tecnico.sec.bftb.server.grpc.ServerServiceGrpc;

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
			"- exit                           Exit the App%n";

	private final PublicKey userPublicKey;
	private final PrivateKey userPrivateKey;
	private final String serverURI;

	public Client(PublicKey userPublicKey, PrivateKey userPrivateKey, String serverURI) {
		this.userPublicKey = userPublicKey;
		this.userPrivateKey = userPrivateKey;
		this.serverURI = serverURI;
	}

	public Client(String userId, String serverURI) {
		this(Resources.getPublicKeyByUserId(userId), Resources.getPrivateKeyByUserId(userId), serverURI);
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

	// Requests sign of life from the server
	public void ping(String input) {
		try {
			PingRequest.Builder builder = PingRequest.newBuilder();
			builder.setInput(input);
			ManagedChannel channel = ManagedChannelBuilder.forTarget(serverURI).usePlaintext().build();
			ServerServiceGrpc.ServerServiceBlockingStub stub = ServerServiceGrpc.newBlockingStub(channel);
			String output = stub.withDeadlineAfter(DEADLINE_SEC, TimeUnit.SECONDS).ping(builder.build()).getOutput();
			System.out.println(output);
		}
		catch (StatusRuntimeException e) {
			System.out.println("ERROR: " + e.getStatus().getDescription());
		}
	}

}