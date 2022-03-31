package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.*;
import pt.tecnico.sec.bftb.server.grpc.Server.TransferFields;
import com.google.protobuf.ByteString;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
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

	private void syncedBackupRelevantState(Account source, Account destination, Transfer transfer)
			throws RestorePreviousStateFailedException {
		try {
			synchronized (Account.getFirstSyncAccount(source, destination)) {
				synchronized (Account.getSecondSyncAccount(source, destination)) {
					synchronized (transfer) {
						backupRelevantState(source, destination, transfer);
					}
				}
			}
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RestorePreviousStateFailedException(e);
		}
	}

	private void backupRelevantState(Account source, Account destination, Transfer transfer)
			throws RestorePreviousStateFailedException {
		int stage = 0;
		try {
			Resources.saveTransfer(transfer);
			stage = 1;
			Resources.saveAccount(source);
			stage = 2;
			Resources.saveAccount(destination);
		}
		catch (TransferSavingFailedException | AccountSavingFailedException e) {
			// If the backup fails, we should revert the changes (This is just an approximation, but it's better than nothing)
			e.printStackTrace();
			if (stage >= 1) transfers.replace(transfer.getID(), Resources.restorePreviousState(transfer));
			if (stage >= 2) accounts.replace(source.getPublicKey(), Resources.restorePreviousState(source));
			throw new RestorePreviousStateFailedException(e);
		}
	}

	private synchronized long getNewIDForTransfer() {
		return ++transferIDCounter;
	}

	// Check Account (part 1):

	public int getBalance(PublicKey publicKey) throws AccountDoesNotExistException {
		Account account = findAccount(publicKey);
		if (account == null) throw new AccountDoesNotExistException();
		synchronized (account) {
			return account.getBalance();
		}
	}

	// Check Account (part 2):

	public List<TransferFields> getPendingTransfers(PublicKey publicKey) throws AccountDoesNotExistException {
		Account account = findAccount(publicKey);
		if (account == null) throw new AccountDoesNotExistException();
		synchronized (account) {
			return buildTransferFields(account.getPendingIncomingTransferIDs());
		}
	}

	public List<TransferFields> buildTransferFields(List<Long> transferIDs) {
		List<TransferFields> transferFieldsList = new ArrayList<TransferFields>();
		for (int i = 0; i < transferIDs.size(); i++) {
			Transfer transfer = transfers.get(transferIDs.get(i));
			synchronized (transfer) {
				TransferFields.Builder builder = TransferFields.newBuilder();
				builder.setSourceKey(ByteString.copyFrom(transfer.getSourceKey().getEncoded()));
				builder.setDestinationKey(ByteString.copyFrom(transfer.getSourceKey().getEncoded()));
				builder.setAmount(transfer.getAmount());
				builder.setPending(transfer.getPending());
				TransferFields transferFields = builder.build();;
				transferFieldsList.add(transferFields);
			}
		}
		return transferFieldsList;
	}

	// Send Amount:

	public void sendAmount(PublicKey sourceKey, PublicKey destinationKey, int amount)
			throws AccountDoesNotExistException, AmountTooLowException, BalanceTooLowException,
			RestorePreviousStateFailedException, NoSuchAlgorithmException {
		if (amount <= 0) throw new AmountTooLowException();
		Account source = findAccount(sourceKey);
		if (source == null) throw new AccountDoesNotExistException();
		Account destination = findAccount(destinationKey);
		if (destination == null) throw new AccountDoesNotExistException();
		synchronized (Account.getFirstSyncAccount(source, destination)) {
			synchronized (Account.getSecondSyncAccount(source, destination)) {
				if (!source.canDecrement(amount)) throw new BalanceTooLowException();
				long newID = getNewIDForTransfer();
				Transfer transfer = new Transfer(newID, sourceKey, destinationKey, amount);
				synchronized (transfer) {
					transfers.put(transfer.getID(), transfer);
					destination.addPendingIncomingTransfer(transfer.getID());
					source.addTransfer(transfer.getID());
					// Backup the state of the relevant objects
					syncedBackupRelevantState(source, destination, transfer);
				}
			}
		}
	}

	// Receive Amount:

	public void receiveAmount(PublicKey publicKey, int transferNum)
			throws AccountDoesNotExistException, BalanceTooLowException, InvalidTransferNumberException,
			RestorePreviousStateFailedException, NoSuchAlgorithmException {
		Account destination = findAccount(publicKey);
		if (destination == null) throw new AccountDoesNotExistException();
		if (!destination.isPendingTransferNumValid(transferNum)) throw new InvalidTransferNumberException();
		long transferID = destination.getPendingTransferID(transferNum);
		Transfer transfer = transfers.get(transferID);
		PublicKey sourceKey = transfer.getSourceKey();
		Account source = findAccount(sourceKey);
		if (source == null) throw new AccountDoesNotExistException();
		int amount = transfer.getAmount();
		synchronized (Account.getFirstSyncAccount(source, destination)) {
			synchronized (Account.getSecondSyncAccount(source, destination)) {
				synchronized (transfer) {
					if (!source.canDecrement(amount)) throw new BalanceTooLowException();
					// TRANSACTION
					source.decrementBalance(amount);
					destination.incrementBalance(amount);
					destination.approveIncomingTransfer(transferNum);
					transfer.approve();
					// Backup the state of the relevant objects
					syncedBackupRelevantState(source, destination, transfer);
				}
			}
		}
	}

	// Audit:

	public List<TransferFields> getApprovedTransfers(PublicKey publicKey) throws AccountDoesNotExistException {
		Account account = findAccount(publicKey);
		if (account == null) throw new AccountDoesNotExistException();
		synchronized (account) {
			return buildTransferFields(account.getTransferIDsHistory());
		}
	}
}
