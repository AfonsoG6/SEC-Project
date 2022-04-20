package pt.tecnico.sec.bftb.client.exceptions;

public class SignatureVerificationFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Verification of signature failed";

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
