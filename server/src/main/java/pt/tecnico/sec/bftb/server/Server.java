package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.*;

import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final ConcurrentHashMap<PublicKey, Account> accounts;
    private long transferIDCounter;

    public Server() throws AccountLoadingFailedException {
        this.accounts = new ConcurrentHashMap<>();
        this.transferIDCounter = 0;
        List<Account> accountsList = Resources.loadAccounts();
        for (Account account : accountsList) {
            accounts.put(account.getPublicKey(), account);
        }
    }

    public void openAccount(PublicKey publicKey) {
        Account newAccount = new Account(publicKey);
        accounts.putIfAbsent(publicKey, newAccount);
        // Backup the state of the relevant objects
        try {
            Resources.saveAccount(newAccount);
        }
        catch (AccountSavingFailedException e) {
            // If the backup fails, we should revert the changes
            // TODO For now we don't
            e.printStackTrace();
        }
    }

    public Account findAccount(PublicKey publicKey) {
        return accounts.get(publicKey);
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
        return account.getPendingIncomingTransfersString();
    }

    // Send Amount:

    public void sendAmount(PublicKey sourceKey, PublicKey destinationKey, int amount) throws AccountDoesNotExistException, AmountTooLowException, BalanceTooLowException {
        if (amount <= 0) throw new AmountTooLowException();
        Account source = findAccount(sourceKey);
        if (source == null) throw new AccountDoesNotExistException();
        Account destination = findAccount(destinationKey);
        if (destination == null) throw new AccountDoesNotExistException();
        if (!source.canDecrement(amount)) throw new BalanceTooLowException();
        long newID = getNewIDForTransfer();
        Transfer transfer = new Transfer(newID, sourceKey, destinationKey, amount);
        destination.addPendingIncomingTransfer(transfer);
        source.addTransfer(transfer);
        // Backup the state of the relevant objects
        try {
            Resources.saveTransfer(transfer);
            Resources.saveAccount(source);
            Resources.saveAccount(destination);
        }
        catch (TransferSavingFailedException | AccountSavingFailedException e) {
            // If the backup fails, we should revert the changes
            // TODO For now we don't
            e.printStackTrace();
        }
    }

    private synchronized long getNewIDForTransfer() {
        return transferIDCounter++;
    }

    // Receive Amount:

    public void receiveAmount(PublicKey publicKey, int transferNum)
            throws AccountDoesNotExistException, BalanceTooLowException, InvalidTransferNumberException {
        Account destination = findAccount(publicKey);
        if (destination == null) throw new AccountDoesNotExistException();
        if (!destination.isPendingTransferNumValid(transferNum)) throw new InvalidTransferNumberException();
        Transfer transfer = destination.getPendingTransfer(transferNum);
        PublicKey sourceKey = transfer.getSourceKey();
        Account source = findAccount(sourceKey);
        if (source == null) throw new AccountDoesNotExistException();
        int amount = transfer.getAmount();
        if (!source.canDecrement(amount)) throw new BalanceTooLowException();
        // TRANSACTION
        source.decrementBalance(amount);
        destination.incrementBalance(amount);
        destination.approveIncomingTransfer(transferNum);
        source.approveOutgoingTransfer(transfer.getID());
        // Backup the state of the relevant objects
        try {
            Resources.saveTransfer(transfer);
            Resources.saveAccount(source);
            Resources.saveAccount(destination);
        }
        catch (TransferSavingFailedException | AccountSavingFailedException e) {
            // If the backup fails, we should revert the changes
            // TODO For now we don't
            e.printStackTrace();
        }
    }

    // Audit:

    public String getApprovedTransfers(PublicKey publicKey) throws AccountDoesNotExistException {
        Account account = findAccount(publicKey);
        if (account == null) throw new AccountDoesNotExistException();
        return account.getTransferHistory();
    }
}
