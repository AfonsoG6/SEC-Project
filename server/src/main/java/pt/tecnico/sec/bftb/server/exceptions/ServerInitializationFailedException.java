package pt.tecnico.sec.bftb.server.exceptions;

public class ServerInitializationFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Server initialization failed";

	public ServerInitializationFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public ServerInitializationFailedException(String message) {
		super(message);
	}

	public ServerInitializationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerInitializationFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
