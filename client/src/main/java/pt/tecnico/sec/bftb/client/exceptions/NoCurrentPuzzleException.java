package pt.tecnico.sec.bftb.client.exceptions;

public class NoCurrentPuzzleException extends Exception {
	private static final String DEFAULT_MESSAGE = "No current puzzle";

	public NoCurrentPuzzleException() {
		super(DEFAULT_MESSAGE);
	}

	public NoCurrentPuzzleException(String message) {
		super(message);
	}

	public NoCurrentPuzzleException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoCurrentPuzzleException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
