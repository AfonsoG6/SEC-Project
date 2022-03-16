package pt.tecnico.sec.bftb.server;

import java.security.PublicKey;
import java.util.ArrayList;

public class Account {

	public static final int INITIAL_BALANCE = 10;

	private PublicKey publicKey;
	private int balance;
	private final ArrayList<Transaction> pending;
	private final ArrayList<Transaction> history;

	public Account(PublicKey publicKey) {
		this.publicKey = publicKey;
		this.balance = INITIAL_BALANCE;
		this.pending = new ArrayList<>();
		this.history = new ArrayList<>();
	}

	public boolean canDecrement(int amount) {
		if (amount > 0 && amount < balance) return true;
		return false;
	}

	public void incrementBalance(int amount) {
		if (amount > 0)
			balance = balance + amount;
	}

	public void decrementBalance(int amount) {
		if (this.canDecrement(amount))
			balance = balance - amount;
	}
}
