package pt.tecnico.sec.bftb.client;

import pt.tecnico.sec.bftb.client.exceptions.KeyPairGenerationFailedException;
import pt.tecnico.sec.bftb.client.exceptions.KeyPairLoadingFailedException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.Scanner;
import java.util.regex.PatternSyntaxException;

public class AppMain {
	public static final String ASK_FOR_HELP = "Use the command 'help' to see the available commands.";
	public static final String ERROR_NUMBER_OF_ARGUMENTS = "ERROR: Invalid number of arguments! " + ASK_FOR_HELP;
	public static final String UNKNOWN_COMMAND = "ERROR: Unknown command! " + ASK_FOR_HELP;
	public static final String HELP_STRING = "Available Commands:%n" +
			"- ping <one word>                Check if server is responsive%n" +
			"- chuser <username>              Change to another user%n" +
			"- open                           Open a new account%n" +
			"- send <destination> <amount>    Send to the destination account the specified amount%n" +
			"- recv                           Confirm the earliest pending incoming transfer%n" +
			"- recv <transfer number>         Confirm the pending incoming transfer with the specified number%n" +
			"- check                          Obtain the balance of the account, and the list of pending transfers%n" +
			"- audit                          Obtain the full transaction history of the account%n" +
			"- exit                           Exit the App%n";
	static Client client;

	public static void main(String[] args)
			throws CertificateException, KeyPairLoadingFailedException, KeyPairGenerationFailedException,
			IOException {
		System.out.println(AppMain.class.getSimpleName());

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check number of arguments
		if (args.length != 3 && args.length != 4) {
			System.out.println("Invalid number of arguments. Aborting!");
			System.out.println("Usage: AppMain <serverHostname> <serverPort> <faultsToTolerate> [inputFile]");
			return;
		}
		// Checks if file was redirected
		boolean hasInputFileArg = (args.length == 4 && args[3] != null && !args[3].isEmpty());

		String svhost = args[0];
		int svport = Integer.parseInt(args[1]);
		int f = Integer.parseInt(args[2]);
		if (f <= 0) {
			System.out.println("Invalid number of faults to tolerate: f > 0");
			return;
		}
		int nTotal = 3*f + 1;

		if (hasInputFileArg) {
			try (InputStream scannerIS = new FileInputStream(args[3])) {
				scanInput(true, scannerIS, svhost, svport, nTotal);
			}
		}

		boolean recvdInputFile = System.console() == null;
		scanInput(recvdInputFile, System.in, svhost, svport, nTotal);
	}

	private static void scanInput(boolean recvdInputFile, InputStream scannerIS, String svhost, int svport, int nTotal)
			throws CertificateException, KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		client = new Client(svhost, svport, nTotal);
		try (Scanner scanner = new Scanner(scannerIS)) {
			System.out.println("--------------------------------------------------------------------------------");
			// We don't want to print a prompt symbol if a file was redirected and there's no line left to consume
			if (!recvdInputFile || scanner.hasNextLine()) System.out.print("> ");

			while (scanner.hasNextLine() || !recvdInputFile) {
				String line = scanner.nextLine();
				// Ignore blank input lines
				if (line.isBlank()) continue;
				// If a file was redirected, print the line consumed
				if (recvdInputFile) System.out.println(line);
				// Parse line (and close App if true)
				if (parseAndExecCommand(line)) return;
				// We don't want to print a prompt symbol if a file was redirected and there's no line left to consume
				if (!recvdInputFile || scanner.hasNextLine()) System.out.print("> ");
			}
		}
	}


	// Returns true if the App should exit
	private static boolean parseAndExecCommand(String line)
			throws KeyPairLoadingFailedException, KeyPairGenerationFailedException {
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
					if (tokens.length == 2) client.ping(tokens[1]);
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "chuser":
					if (tokens.length == 2) client.changeUser(tokens[1]);
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "open":
					if (tokens.length == 1) client.openAccount();
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "send":
					if (tokens.length == 3) client.sendAmount(tokens[1], Integer.parseInt(tokens[2]));
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "check":
					if (tokens.length == 1) client.checkAccount();
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "recv":
					if (tokens.length == 1) client.receiveAmount(0);
					else if (tokens.length == 2) client.receiveAmount(Integer.parseInt(tokens[1]));
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				case "audit":
					if (tokens.length == 1) client.audit();
					else System.out.println(ERROR_NUMBER_OF_ARGUMENTS);
					break;
				default:
					System.out.println(UNKNOWN_COMMAND);
					break;
			}
		}
		catch (NumberFormatException | PatternSyntaxException e) {
			System.out.println(UNKNOWN_COMMAND);
		}
		// Print separator
		System.out.println("--------------------------------------------------------------------------------");
		return false;
	}
}
