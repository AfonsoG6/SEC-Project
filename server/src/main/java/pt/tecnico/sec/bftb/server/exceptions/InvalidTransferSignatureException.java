package pt.tecnico.sec.bftb.server.exceptions;

public class InvalidTransferSignatureException extends Exception {
	private static final String DEFAULT_MESSAGE = "Invalid transfer signature";

	public InvalidTransferSignatureException() {
		super(DEFAULT_MESSAGE);
	}

	public InvalidTransferSignatureException(String message) {
		super(message);
	}

	public InvalidTransferSignatureException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidTransferSignatureException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
