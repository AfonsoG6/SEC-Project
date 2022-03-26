package pt.tecnico.sec.bftb.server.exceptions;

public class TransferSavingFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Transfer saving failed";

	public TransferSavingFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public TransferSavingFailedException(String message) {
		super(message);
	}

	public TransferSavingFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransferSavingFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
