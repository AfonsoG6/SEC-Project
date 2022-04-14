package pt.tecnico.sec.bftb.server.exceptions;

public class InvalidNewBalanceException extends Exception {
	private static final String DEFAULT_MESSAGE = "Invalid new balance";

	public InvalidNewBalanceException() {
		super(DEFAULT_MESSAGE);
	}

	public InvalidNewBalanceException(String message) {
		super(message);
	}

	public InvalidNewBalanceException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidNewBalanceException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
