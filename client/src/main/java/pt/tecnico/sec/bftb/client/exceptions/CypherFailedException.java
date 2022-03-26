package pt.tecnico.sec.bftb.client.exceptions;

public class CypherFailedException extends Exception {
	private static final String DEFAULT_MESSAGE = "Cypher failed";

	public CypherFailedException() {
		super(DEFAULT_MESSAGE);
	}

	public CypherFailedException(String message) {
		super(message);
	}

	public CypherFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public CypherFailedException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
