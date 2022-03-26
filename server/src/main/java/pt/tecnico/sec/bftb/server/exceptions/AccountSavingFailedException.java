package pt.tecnico.sec.bftb.server.exceptions;

public class AccountSavingFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Account saving failed";

	public AccountSavingFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public AccountSavingFailedException(String message) {
		super(message);
	}

	public AccountSavingFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccountSavingFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
