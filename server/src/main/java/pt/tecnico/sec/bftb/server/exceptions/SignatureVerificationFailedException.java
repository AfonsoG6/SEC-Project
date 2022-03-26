package pt.tecnico.sec.bftb.server.exceptions;

public class SignatureVerificationFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Signature verification failed";

	public SignatureVerificationFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public SignatureVerificationFailedException(String message) {
		super(message);
	}

	public SignatureVerificationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public SignatureVerificationFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
