package pt.tecnico.sec.bftb.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.sec.bftb.server.exceptions.ServerInitializationFailedException;

import java.io.IOException;

public class ServerMain {

	public static void main(String[] args)
			throws IOException, InterruptedException, ServerInitializationFailedException {
		System.out.println(ServerMain.class.getSimpleName());

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check number of arguments
		if (args.length != 3) {
			System.err.println("Invalid number of arguments. Aborting!");
			System.err.println("Usage: ServerMain <port> <n> <f>");
			return;
		}

		int port = Integer.parseInt(args[0]);

		int f = Integer.parseInt(args[1]); // Byzantine faults to be tolerated
		if (f <= 0) {
			System.out.println("Invalid number of faults to tolerate: f > 0");
			return;
		}

		int n = Integer.parseInt(args[2]); // Replica index
		if (!((n >= 0) && (n < (3 * f + 1)))) {
			System.err.println("Invalid replica index: n >= 0 && n < 3f + 1");
			System.err.println("Usage: ServerMain <port> <n> <f>");
			return;
		}

		port = port + n;

		// Create Service
		final ServerServiceImpl service = new ServerServiceImpl(n);
		// Setup and start server
		Server server = ServerBuilder.forPort(port).addService(service).build();
		server.start();
		System.out.println("Server started");
		server.awaitTermination();
	}
}
