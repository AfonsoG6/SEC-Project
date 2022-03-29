package pt.tecnico.sec.bftb.client.exceptions;

public class CertificateGenerationFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Certificate generation failed";

	public CertificateGenerationFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public CertificateGenerationFailedException(String message) {
		super(message);
	}

	public CertificateGenerationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public CertificateGenerationFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
