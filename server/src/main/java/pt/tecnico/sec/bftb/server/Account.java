package pt.tecnico.sec.bftb.server;

import java.security.PublicKey;
import java.util.ArrayList;

public class Account {

	public static final int INITIAL_BALANCE = 10;

	private PublicKey publicKey;
	private int balance;
	private final ArrayList<Transfer> pendingTransfers;
	private final ArrayList<Transfer> approvedTransfers;

	public Account(PublicKey publicKey) {
		this.publicKey = publicKey;
		this.balance = INITIAL_BALANCE;
		this.pendingTransfers = new ArrayList<>();
		this.approvedTransfers = new ArrayList<>();
	}

	// Returns the account's balance

	public int getBalance() {
		return balance;
	}

	// Checks if a given amount can be decremented from the account's balance

	public boolean canDecrement(int amount) {
		if (amount > 0 && amount < balance) return true;
		return false;
	}

	// Increments the account's balance by a given amount

	public void incrementBalance(int amount) {
		if (amount > 0)
			balance = balance + amount;
	}

	// Decrements the account's balance by a given amount

	public void decrementBalance(int amount) {
		if (this.canDecrement(amount))
			balance = balance - amount;
	}

	// Adds a new pending transfer from a given sourceKey and amount

	public void addPendingTransfer(PublicKey sourceKey, int amount) {
		pendingTransfers.add(new Transfer(sourceKey, this.publicKey, amount));
	}

	// Returns a pending transfer specified by an index

	public Transfer getPendingTransfer(int i) {
		return pendingTransfers.get(i);
	}

	// Returns all pending transfers concatenated into a string

	public String getPendingTransfers() {
		String pT = "";
		for (int i = 0; i < pendingTransfers.size(); i++) {
			pT += "TRANSFER " + i + ": " + this.getPendingTransfer(i).toString() + "\n";
		}
		return pT;
	}

	public void approveTransfer(int i) {
		Transfer transfer = this.getPendingTransfer(i);
		pendingTransfers.remove(i);
		transfer.approve();
		approvedTransfers.add(transfer);
	}

	public boolean isPendingTransferNumValid(int num) {
		return num >= 0 && num < this.pendingTransfers.size();
	}
}
