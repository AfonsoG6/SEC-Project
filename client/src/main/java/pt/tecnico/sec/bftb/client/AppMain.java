package pt.tecnico.sec.bftb.client;

import java.util.Scanner;

public class AppMain {
	public static final String HUB_PATH = "/grpc/bicloin/hub/1";

	public static void main(String[] args) {
		System.out.println(AppMain.class.getSimpleName());

		// Check number of arguments
		if (args.length != 6) {
			System.out.println("Invalid number of arguments received.%nUsage: app <serverHostname> <serverPort> <userID>");
			return;
		}
		// Checks if file was redirected
		boolean fileRed = (System.console() == null);

		String userID = args[2];
		String serverURI = args[0] + ":" + args[1];

		Client client = new Client(userID, serverURI);
		try (Scanner scanner = new Scanner(System.in)) {
			// We don't want to print a prompt symbol if a file was redirected and there's no line left to consume
			if (!fileRed || scanner.hasNextLine()) System.out.print("> ");

			while (scanner.hasNextLine() || !fileRed) {
				String line = scanner.nextLine();
				// Ignore blank input lines
				if (line.isBlank()) continue;
				// If a file was redirected, print the line consumed
				if (fileRed) System.out.println(line);
				// Parse line (and close App if true)
				if (client.parseAndExecCommand(line)) return;
				// We don't want to print a prompt symbol if a file was redirected and there's no line left to consume
				if (!fileRed || scanner.hasNextLine()) System.out.print("> ");
			}
		}
	}
}
