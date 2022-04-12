package pt.tecnico.sec.bftb.server.exceptions;

public class ConnectionToDatabaseFailed extends Exception {
	private static final String DEFAULT_MESSAGE = "Connection to database failed";

	public ConnectionToDatabaseFailed() {
		super(DEFAULT_MESSAGE);
	}

	public ConnectionToDatabaseFailed(String message) {
		super(message);
	}

	public ConnectionToDatabaseFailed(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionToDatabaseFailed(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
