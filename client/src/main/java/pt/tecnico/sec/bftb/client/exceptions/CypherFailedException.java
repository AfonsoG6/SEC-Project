package pt.tecnico.sec.bftb.client.exceptions;

public class CypherFailedException extends Exception {
	public CypherFailedException() {
		super();
	}

	public CypherFailedException(String message) {
		super(message);
	}

	public CypherFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public CypherFailedException(Throwable cause) {
		super(cause);
	}
}
