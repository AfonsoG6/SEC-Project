package pt.tecnico.sec.bftb.server.exceptions;

public class KeepPreviousStateFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Keeping previous state failed";

	public KeepPreviousStateFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public KeepPreviousStateFailedException(String message) {
		super(message);
	}

	public KeepPreviousStateFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}

	public KeepPreviousStateFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
