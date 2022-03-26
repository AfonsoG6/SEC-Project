package pt.tecnico.sec.bftb.server.exceptions;

public class TransferLoadingFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Transfer loading failed";

	public TransferLoadingFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public TransferLoadingFailedException(String message) {
		super(message);
	}

	public TransferLoadingFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransferLoadingFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
