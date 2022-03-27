package pt.tecnico.sec.bftb.server.exceptions;

public class AccountAlreadyExistsException extends Exception {
	private static final String DEFAULT_MESSAGE = "Account already exists";

	public AccountAlreadyExistsException() {
		super(DEFAULT_MESSAGE);
	}

	public AccountAlreadyExistsException(String message) {
		super(message);
	}

	public AccountAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccountAlreadyExistsException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
