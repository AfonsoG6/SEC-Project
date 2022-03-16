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
}
