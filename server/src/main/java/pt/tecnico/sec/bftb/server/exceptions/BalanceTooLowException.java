package pt.tecnico.sec.bftb.server.exceptions;

public class BalanceTooLowException extends Exception {
	private static final String DEFAULT_MESSAGE = "Balance of the account is too low";

	public BalanceTooLowException() {
		super(DEFAULT_MESSAGE);
	}

	public BalanceTooLowException(String message) {
		super(message);
	}

	public BalanceTooLowException(String message, Throwable cause) {
		super(message, cause);
	}

	public BalanceTooLowException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
