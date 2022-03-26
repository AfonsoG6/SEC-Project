package pt.tecnico.sec.bftb.server.exceptions;

public class AmountTooLowException extends Exception {
	private static final String DEFAULT_MESSAGE = "Amount requested for transfer is too low";

	public AmountTooLowException() {
		super(DEFAULT_MESSAGE);
	}

	public AmountTooLowException(String message) {
		super(message);
	}

	public AmountTooLowException(String message, Throwable cause) {
		super(message, cause);
	}

	public AmountTooLowException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
