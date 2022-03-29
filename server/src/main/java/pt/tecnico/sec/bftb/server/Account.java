package pt.tecnico.sec.bftb.server;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account implements Serializable {
	@Serial
	private static final long serialVersionUID = 202203261536L;
	private static final int INITIAL_BALANCE = 100;

	private final PublicKey publicKey;
	private final ArrayList<Long> pendingIncomingTransfers;
	private final ArrayList<Long> transferHistory;
	private int balance;

	public Account(PublicKey publicKey) {
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

	public void addPendingIncomingTransfer(long transferID) {
		pendingIncomingTransfers.add(transferID);
	}

	public void addTransfer(long transferID) {
		transferHistory.add(transferID);
	}

	// Returns all pending transfers concatenated into a string

	public List<Long> getPendingIncomingTransferIDs() {
		return Collections.unmodifiableList(pendingIncomingTransfers);
	}

	public Long getPendingTransferID(int transferNum) {
		return pendingIncomingTransfers.get(transferNum);
	}

	public void approveIncomingTransfer(int i) {
		long transferID = pendingIncomingTransfers.get(i);
		pendingIncomingTransfers.remove(i);
		transferHistory.add(transferID);
	}

	public boolean isPendingTransferNumValid(int num) {
		return num >= 0 && num < pendingIncomingTransfers.size();
	}

	// Returns transfer history concatenated into a string

	public List<Long> getTransferIDsHistory() {
		return Collections.unmodifiableList(transferHistory);
	}

	public byte[] getHash() throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
	}

	public BigInteger getBigIntHash() throws NoSuchAlgorithmException {
		return new BigInteger(1, getHash());
	}

	public String getStringHash() throws NoSuchAlgorithmException {
		return String.format("%064x", getBigIntHash());
	}

	// To prevent deadlocks, we define a way to compare accounts based on the hash of their public keys:

	public static Account getFirstSyncAccount(Account a1, Account a2) throws NoSuchAlgorithmException {
		if (a1.getBigIntHash().compareTo(a2.getBigIntHash()) < 0)
			return a1;
		else
			return a2;
	}

	public static Account getSecondSyncAccount(Account a1, Account a2) throws NoSuchAlgorithmException {
		if (a1.getBigIntHash().compareTo(a2.getBigIntHash()) < 0)
			return a2;
		else
			return a1;
	}

}
