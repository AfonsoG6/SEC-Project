package pt.tecnico.sec.bftb.server.exceptions;

public class AmountTooLowException extends Exception {
	public AmountTooLowException() {
		super();
	}

	public AmountTooLowException(String message) {
		super(message);
	}

	public AmountTooLowException(String message, Throwable cause) {
		super(message, cause);
	}

	public AmountTooLowException(Throwable cause) {
		super(cause);
	}
}
