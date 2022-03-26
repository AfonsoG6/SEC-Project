package pt.tecnico.sec.bftb.client.exceptions;

public class NonceRequestFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Nonce request failed";

	public NonceRequestFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public NonceRequestFailedException(String message) {
		super(message);
	}

	public NonceRequestFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public NonceRequestFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
