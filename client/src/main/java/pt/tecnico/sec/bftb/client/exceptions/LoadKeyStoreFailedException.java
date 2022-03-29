package pt.tecnico.sec.bftb.client.exceptions;

public class LoadKeyStoreFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Load keystore failed";

	public LoadKeyStoreFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public LoadKeyStoreFailedException(String message) {
		super(message);
	}

	public LoadKeyStoreFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}

	public LoadKeyStoreFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
