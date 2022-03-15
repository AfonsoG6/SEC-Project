package pt.tecnico.sec.server;

import java.security.PublicKey;

public class Account {
	private String name;
	private PublicKey publicKey;

	public Account(String name, PublicKey publicKey) {
		this.name = name;
		this.publicKey = publicKey;
	}
}
