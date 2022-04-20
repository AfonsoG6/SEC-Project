package pt.tecnico.sec.bftb.client.exceptions;

public class InvalidSignatureException extends Exception {
	private static final String DEFAULT_MESSAGE = "Invalid signature";

	public InvalidSignatureException() {
		super(DEFAULT_MESSAGE);
	}

	public InvalidSignatureException(String message) {
		super(message);
	}

	public InvalidSignatureException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidSignatureException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
