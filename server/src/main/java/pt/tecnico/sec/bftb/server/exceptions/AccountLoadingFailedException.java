package pt.tecnico.sec.bftb.server.exceptions;

public class AccountLoadingFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Account loading failed";

	public AccountLoadingFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public AccountLoadingFailedException(String message) {
		super(message);
	}

	public AccountLoadingFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccountLoadingFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
