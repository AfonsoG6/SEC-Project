package pt.tecnico.sec.bftb.server.exceptions;

public class RestorePreviousStateFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Restore previous state failed";

	public RestorePreviousStateFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public RestorePreviousStateFailedException(String message) {
		super(message);
	}

	public RestorePreviousStateFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public RestorePreviousStateFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
