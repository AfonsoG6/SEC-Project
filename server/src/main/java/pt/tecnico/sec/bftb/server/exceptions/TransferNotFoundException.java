package pt.tecnico.sec.bftb.server.exceptions;

public class TransferNotFoundException extends Exception {
	private static final String DEFAULT_MESSAGE = "Transfer was not found";

	public TransferNotFoundException() {
		super(DEFAULT_MESSAGE);
	}

	public TransferNotFoundException(String message) {
		super(message);
	}

	public TransferNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransferNotFoundException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
