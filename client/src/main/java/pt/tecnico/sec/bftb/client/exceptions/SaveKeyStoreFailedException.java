package pt.tecnico.sec.bftb.client.exceptions;

public class SaveKeyStoreFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Save keystore failed";

	public SaveKeyStoreFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public SaveKeyStoreFailedException(String message) {
		super(message);
	}

	public SaveKeyStoreFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}

	public SaveKeyStoreFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
