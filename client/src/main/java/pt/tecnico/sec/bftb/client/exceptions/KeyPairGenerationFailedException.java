package pt.tecnico.sec.bftb.client.exceptions;

public class KeyPairGenerationFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Key pair generation failed";

	public KeyPairGenerationFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public KeyPairGenerationFailedException(String message) {
		super(message);
	}

	public KeyPairGenerationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public KeyPairGenerationFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
