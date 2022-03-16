package pt.tecnico.sec.server;

import java.security.PublicKey;

public class Account {
	private PublicKey publicKey;
	private int balance;

	public Account(PublicKey publicKey, int balance) {
		this.publicKey = publicKey;
		this.balance = balance;
	}
}
