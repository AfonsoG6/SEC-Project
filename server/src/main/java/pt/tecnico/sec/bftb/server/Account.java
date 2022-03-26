package pt.tecnico.sec.bftb.server;

import java.io.Serial;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;

public class Account implements Serializable {
	@Serial
	private static final long serialVersionUID = 202203261536L;
	private static final int INITIAL_BALANCE = 100;

	private final PublicKey publicKey;
	private int balance;
	private final ArrayList<Transfer> pendingIncomingTransfers;
	private final ArrayList<Transfer> transferHistory;

	public Account(PublicKey publicKey){
		this.publicKey = publicKey;
		this.balance = INITIAL_BALANCE;
		this.pendingIncomingTransfers = new ArrayList<>();
		this.transferHistory = new ArrayList<>();
	}

	// Returns the account's balance

	public int getBalance() {
		return balance;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	// Checks if a given amount can be decremented from the account's balance

	public boolean canDecrement(int amount) {
		return amount > 0 && amount < balance;
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

	public void addPendingIncomingTransfer(Transfer transfer) {
		pendingIncomingTransfers.add(transfer);
	}

	public void addTransfer(Transfer transfer) {
		transferHistory.add(transfer);
	}

	// Returns all pending transfers concatenated into a string

	public String getPendingIncomingTransfersString() {
		String pendingTransfersString = "";
		for (int i = 0; i < pendingIncomingTransfers.size(); i++) {
			pendingTransfersString += "INCOMING TRANSFER " + i + ": " + pendingIncomingTransfers.get(i).toString() + "\n";
		}
		return pendingTransfersString;
	}

	public Transfer getPendingTransfer(int transferNum) {
		return pendingIncomingTransfers.get(transferNum);
	}

	public void approveIncomingTransfer(int i) {
		Transfer transfer = pendingIncomingTransfers.get(i);
		pendingIncomingTransfers.remove(i);
		transfer.approve();
		transferHistory.add(transfer);
	}

	public void approveOutgoingTransfer(long transferID) {
		transferHistory.stream()
				.filter(transfer -> transfer.getID() == transferID)
				.findFirst()
				.ifPresent(Transfer::approve);
	}

	public boolean isPendingTransferNumValid(int num) {
		return num >= 0 && num < pendingIncomingTransfers.size();
	}

	// Returns transfer history concatenated into a string

	public String getTransferHistory() {
		StringBuilder historyString = new StringBuilder();
		for (int i = 0; i < transferHistory.size(); i++) {
			Transfer transfer = transferHistory.get(i);
			String direction = (publicKey.equals(transfer.getSourceKey()))? "OUTGOING" : "INCOMING";
			historyString.append("%s TRANSFER %d: %s%n".formatted(direction, i, transfer.toString()));
		}
		return historyString.toString();
	}

	public String getHash() throws NoSuchAlgorithmException {
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
		return String.format("%064x", new java.math.BigInteger(1, hash));
	}
}
