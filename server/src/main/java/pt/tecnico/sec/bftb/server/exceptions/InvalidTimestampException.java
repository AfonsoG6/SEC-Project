package pt.tecnico.sec.bftb.server.exceptions;

public class InvalidTimestampException extends Exception {
	private static final String DEFAULT_MESSAGE = "Invalid timestamp";

	public InvalidTimestampException() {
		super(DEFAULT_MESSAGE);
	}

	public InvalidTimestampException(String message) {
		super(message);
	}

	public InvalidTimestampException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}

	public InvalidTimestampException(String message, Throwable cause) {
		super(message, cause);
	}
}
