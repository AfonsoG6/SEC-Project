package pt.tecnico.sec.bftb.server.exceptions;

public class SignatureVerificationFailedException extends Exception {
	public SignatureVerificationFailedException() {
		super();
	}

	public SignatureVerificationFailedException(String message) {
		super(message);
	}

	public SignatureVerificationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public SignatureVerificationFailedException(Throwable cause) {
		super(cause);
	}
}
