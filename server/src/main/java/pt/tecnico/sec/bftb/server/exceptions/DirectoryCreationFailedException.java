package pt.tecnico.sec.bftb.server.exceptions;

public class DirectoryCreationFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Directory creation failed";

	public DirectoryCreationFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public DirectoryCreationFailedException(String message) {
		super(message);
	}

	public DirectoryCreationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DirectoryCreationFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
