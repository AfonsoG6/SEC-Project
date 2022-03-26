package pt.tecnico.sec.bftb.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.sec.bftb.server.exceptions.AccountLoadingFailedException;
import pt.tecnico.sec.bftb.server.exceptions.PrivateKeyLoadingFailedException;

import java.io.IOException;

public class ServerMain {

	public static void main(String[] args)
			throws IOException, InterruptedException, PrivateKeyLoadingFailedException, AccountLoadingFailedException {
		System.out.println(ServerMain.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check number of arguments
		if (args.length != 1) {
			System.err.println("Invalid number of arguments. Aborting!");
			System.err.println("Usage: ServerMain <port>");
			return;
		}

		int port = Integer.parseInt(args[0]);

		// Create Service
		final ServerServiceImpl service = new ServerServiceImpl();
		// Setup and start server
		Server server = ServerBuilder.forPort(port).addService(service).build();
		server.start();
		System.out.println("Server started");
		server.awaitTermination();
	}
}
