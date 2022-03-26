package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.AccountDoesNotExistException;
import pt.tecnico.sec.bftb.server.exceptions.AmountTooLowException;
import pt.tecnico.sec.bftb.server.exceptions.BalanceTooLowException;
import pt.tecnico.sec.bftb.server.exceptions.InvalidTransferNumberException;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final ConcurrentHashMap<PublicKey, Account> accounts;

    public Server() {
        this.accounts = new ConcurrentHashMap<>();
    }

    public void openAccount(PublicKey publicKey) {
        accounts.putIfAbsent(publicKey, new Account(publicKey));
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
        return account.getPendingTransfers();
    }

    // Send Amount:

    public void sendAmount(PublicKey sourceKey, PublicKey destinationKey, int amount) throws AccountDoesNotExistException, AmountTooLowException, BalanceTooLowException {
        if (amount <= 0) throw new AmountTooLowException();
        Account source = findAccount(sourceKey);
        if (source == null) throw new AccountDoesNotExistException();
        Account destination = findAccount(destinationKey);
        if (destination == null) throw new AccountDoesNotExistException();
        if (!source.canDecrement(amount)) throw new BalanceTooLowException();
        destination.addPendingTransfer(sourceKey, amount);
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
        destination.approveTransfer(0);
    }

    // Audit:

    public String getApprovedTransfers(PublicKey publicKey) throws AccountDoesNotExistException {
        Account account = findAccount(publicKey);
        if (account == null) throw new AccountDoesNotExistException();
        return account.getApprovedTransfers();
    }
}
