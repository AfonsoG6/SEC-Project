package pt.tecnico.sec.bftb.server.exceptions;

public class PrivateKeyLoadingFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Private key loading failed";

	public PrivateKeyLoadingFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public PrivateKeyLoadingFailedException(String message) {
		super(message);
	}

	public PrivateKeyLoadingFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public PrivateKeyLoadingFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
