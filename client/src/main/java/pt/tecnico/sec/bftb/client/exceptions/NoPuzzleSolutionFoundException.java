package pt.tecnico.sec.bftb.client.exceptions;

public class NoPuzzleSolutionFoundException extends Exception {
	private static final String DEFAULT_MESSAGE = "No Puzzle solution found";

	public NoPuzzleSolutionFoundException() {
		super(DEFAULT_MESSAGE);
	}

	public NoPuzzleSolutionFoundException(String message) {
		super(message);
	}

	public NoPuzzleSolutionFoundException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}

	public NoPuzzleSolutionFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
