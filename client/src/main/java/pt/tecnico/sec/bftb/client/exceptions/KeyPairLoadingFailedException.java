package pt.tecnico.sec.bftb.client.exceptions;

public class KeyPairLoadingFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Key pair loading failed";

	public KeyPairLoadingFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public KeyPairLoadingFailedException(String message) {
		super(message);
	}

	public KeyPairLoadingFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public KeyPairLoadingFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
