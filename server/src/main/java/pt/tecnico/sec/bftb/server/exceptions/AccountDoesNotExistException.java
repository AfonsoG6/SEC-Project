package pt.tecnico.sec.bftb.server.exceptions;

public class AccountDoesNotExistException extends Exception {
	private static final String DEFAULT_MESSAGE = "Account associated with given Public Key does not exist";

	public AccountDoesNotExistException() {
		super(DEFAULT_MESSAGE);
	}

	public AccountDoesNotExistException(String message) {
		super(message);
	}

	public AccountDoesNotExistException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccountDoesNotExistException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
