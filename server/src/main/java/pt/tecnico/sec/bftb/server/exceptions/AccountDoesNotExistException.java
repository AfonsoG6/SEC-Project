package pt.tecnico.sec.bftb.server.exceptions;

public class AccountDoesNotExistException extends Exception {
	public AccountDoesNotExistException() {
		super();
	}

	public AccountDoesNotExistException(String message) {
		super(message);
	}

	public AccountDoesNotExistException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccountDoesNotExistException(Throwable cause) {
		super(cause);
	}
}
