package pt.tecnico.sec.bftb.server.exceptions;

public class AccountDoesNotHavePuzzleException extends Exception {
	private static final String DEFAULT_MESSAGE = "Account does not have a puzzle";

	public AccountDoesNotHavePuzzleException() {
		super(DEFAULT_MESSAGE);
	}

	public AccountDoesNotHavePuzzleException(String message) {
		super(message);
	}

	public AccountDoesNotHavePuzzleException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccountDoesNotHavePuzzleException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
