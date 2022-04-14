package pt.tecnico.sec.bftb.client.exceptions;

public class NotEnoughValidResponsesException extends Exception {
	private static final String DEFAULT_MESSAGE = "Not enough valid responses";

	public NotEnoughValidResponsesException() {
		super(DEFAULT_MESSAGE);
	}

	public NotEnoughValidResponsesException(String message) {
		super(message);
	}

	public NotEnoughValidResponsesException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotEnoughValidResponsesException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
