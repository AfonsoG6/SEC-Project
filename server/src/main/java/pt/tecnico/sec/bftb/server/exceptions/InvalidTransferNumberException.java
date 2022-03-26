package pt.tecnico.sec.bftb.server.exceptions;

public class InvalidTransferNumberException extends Exception {
	private static final String DEFAULT_MESSAGE = "Invalid transfer number";

	public InvalidTransferNumberException() {
		super(DEFAULT_MESSAGE);
	}

	public InvalidTransferNumberException(String message) {
		super(message);
	}

	public InvalidTransferNumberException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidTransferNumberException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
