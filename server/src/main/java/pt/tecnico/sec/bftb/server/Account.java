package pt.tecnico.sec.bftb.server;

import java.security.PublicKey;

public class Account {

	public static final int INITIAL_BALANCE = 10;

	private PublicKey publicKey;
	private int balance;

	public Account(PublicKey publicKey) {
		this.publicKey = publicKey;
		this.balance = INITIAL_BALANCE;
	}

	public boolean canDecrement(int value) {
		if (value > 0 && value < balance) return true;
		return false;
	}

	public void incrementBalance(int value) {
		if (value > 0)
			balance = balance + value;
	}

	public void decrementBalance(int value) {
		if (this.canDecrement(value))
			balance = balance - value;
	}
}
