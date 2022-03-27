package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.*;

import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
	private final ConcurrentHashMap<PublicKey, Account> accounts;
	private final ConcurrentHashMap<Long, Transfer> transfers;
	private long transferIDCounter;

	public Server() throws ServerInitializationFailedException {
		try {
			Resources.init();
			this.accounts = new ConcurrentHashMap<>();
			List<Account> accountsList = Resources.loadAccounts();
			for (Account account : accountsList) {
				this.accounts.put(account.getPublicKey(), account);
			}
			this.transferIDCounter = 0;
			this.transfers = new ConcurrentHashMap<>();
			List<Transfer> transfersList = Resources.loadTransfers();
			for (Transfer transfer : transfersList) {
				this.transferIDCounter = Math.max(this.transferIDCounter, transfer.getID());
				this.transfers.put(transfer.getID(), transfer);
			}
		}
		catch (AccountLoadingFailedException | DirectoryCreationFailedException | TransferLoadingFailedException e) {
			throw new ServerInitializationFailedException(e);
		}
	}

	public void openAccount(PublicKey publicKey) throws AccountAlreadyExistsException, AccountSavingFailedException {
		Account newAccount = new Account(publicKey);
		if (accounts.containsKey(publicKey)) {
			throw new AccountAlreadyExistsException();
		}
		accounts.put(publicKey, newAccount);
		// Backup the state of the relevant objects
		Resources.saveAccount(newAccount);
	}

	public Account findAccount(PublicKey publicKey) {
		return accounts.get(publicKey);
	}

	private void backupRelevantState(Account source, Account destination, Transfer transfer)
			throws RestorePreviousStateFailedException {
		try {
			Resources.saveTransfer(transfer);
			Resources.saveAccount(source);
			Resources.saveAccount(destination);
		}
		catch (TransferSavingFailedException | AccountSavingFailedException e) {
			// If the backup fails, we should revert the changes
			e.printStackTrace();
			transfers.replace(transfer.getID(), Resources.restorePreviousState(transfer));
			accounts.replace(source.getPublicKey(), Resources.restorePreviousState(source));
			accounts.replace(destination.getPublicKey(), Resources.restorePreviousState(destination));
		}
	}

	private synchronized long getNewIDForTransfer() {
		return ++transferIDCounter;
	}

	// Check Account (part 1):

	public int getBalance(PublicKey publicKey) throws AccountDoesNotExistException {
		Account account = findAccount(publicKey);
		if (account == null) throw new AccountDoesNotExistException();
		return account.getBalance();
	}

	// Check Account (part 2):

	public String getPendingTransfers(PublicKey publicKey) throws AccountDoesNotExistException {
		Account account = findAccount(publicKey);
		if (account == null) throw new AccountDoesNotExistException();
		return buildPendingTransfersString(account.getPendingIncomingTransferIDs());
	}

	public String buildPendingTransfersString(List<Long> transferIDs) {
		StringBuilder pendingTransfersString = new StringBuilder();
		for (int i = 0; i < transferIDs.size(); i++) {
			Transfer transfer = transfers.get(transferIDs.get(i));
			pendingTransfersString.append("INCOMING TRANSFER no.%d: %s%n".formatted(i, transfer.toString()));
		}
		return pendingTransfersString.toString();
	}

	// Send Amount:

	public void sendAmount(PublicKey sourceKey, PublicKey destinationKey, int amount)
			throws AccountDoesNotExistException, AmountTooLowException, BalanceTooLowException,
			RestorePreviousStateFailedException {
		if (amount <= 0) throw new AmountTooLowException();
		Account source = findAccount(sourceKey);
		if (source == null) throw new AccountDoesNotExistException();
		Account destination = findAccount(destinationKey);
		if (destination == null) throw new AccountDoesNotExistException();
		if (!source.canDecrement(amount)) throw new BalanceTooLowException();
		long newID = getNewIDForTransfer();
		Transfer transfer = new Transfer(newID, sourceKey, destinationKey, amount);
		transfers.put(transfer.getID(), transfer);
		destination.addPendingIncomingTransfer(transfer.getID());
		source.addTransfer(transfer.getID());
		// Backup the state of the relevant objects
		backupRelevantState(source, destination, transfer);
	}

	// Receive Amount:

	public void receiveAmount(PublicKey publicKey, int transferNum)
			throws AccountDoesNotExistException, BalanceTooLowException, InvalidTransferNumberException,
			RestorePreviousStateFailedException {
		Account destination = findAccount(publicKey);
		if (destination == null) throw new AccountDoesNotExistException();
		if (!destination.isPendingTransferNumValid(transferNum)) throw new InvalidTransferNumberException();
		long transferID = destination.getPendingTransferID(transferNum);
		Transfer transfer = transfers.get(transferID);
		PublicKey sourceKey = transfer.getSourceKey();
		Account source = findAccount(sourceKey);
		if (source == null) throw new AccountDoesNotExistException();
		int amount = transfer.getAmount();
		if (!source.canDecrement(amount)) throw new BalanceTooLowException();
		// TRANSACTION
		source.decrementBalance(amount);
		destination.incrementBalance(amount);
		destination.approveIncomingTransfer(transferNum);
		this.transfers.get(transferID).approve();
		// Backup the state of the relevant objects
		backupRelevantState(source, destination, transfer);
	}

	// Audit:

	public String getApprovedTransfers(PublicKey publicKey) throws AccountDoesNotExistException {
		Account account = findAccount(publicKey);
		if (account == null) throw new AccountDoesNotExistException();
		return buildTransferHistoryString(account.getPublicKey(), account.getTransferIDsHistory());
	}

	private String buildTransferHistoryString(PublicKey userPublicKey, List<Long> transferIDs) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < transferIDs.size(); i++) {
			Long transferID = transferIDs.get(i);
			Transfer transfer = this.transfers.get(transferID);
			String direction = (userPublicKey.equals(transfer.getSourceKey())) ? "OUTGOING" : "INCOMING";
			builder.append("%s TRANSFER no.%d: %s%n".formatted(direction, i, transfer.toString()));
		}
		return builder.toString();
	}
}
