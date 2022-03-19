package pt.tecnico.sec.bftb.server.exceptions;

public class BalanceTooLowException extends Exception {
	public BalanceTooLowException() {
		super();
	}

	public BalanceTooLowException(String message) {
		super(message);
	}

	public BalanceTooLowException(String message, Throwable cause) {
		super(message, cause);
	}

	public BalanceTooLowException(Throwable cause) {
		super(cause);
	}
}
