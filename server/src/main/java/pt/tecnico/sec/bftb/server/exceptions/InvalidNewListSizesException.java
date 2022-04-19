package pt.tecnico.sec.bftb.server.exceptions;

public class InvalidNewListSizesException extends Exception {
	private static final String DEFAULT_MESSAGE = "Invalid new list sizes";

	public InvalidNewListSizesException() {
		super(DEFAULT_MESSAGE);
	}

	public InvalidNewListSizesException(String message) {
		super(message);
	}

	public InvalidNewListSizesException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidNewListSizesException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
