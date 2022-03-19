package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.AccountDoesNotExistException;
import pt.tecnico.sec.bftb.server.exceptions.AmountTooLowException;
import pt.tecnico.sec.bftb.server.exceptions.BalanceTooLowException;
import pt.tecnico.sec.bftb.server.exceptions.InvalidRequestException;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final ConcurrentHashMap<PublicKey, Account> accounts;

    public Server() {
        accounts = new ConcurrentHashMap<>();
    }

    public Account findAccount(PublicKey publicKey) {
        return accounts.get(publicKey);
    }

    public Account getAccount(PublicKey publicKey) throws AccountDoesNotExistException {
        Account account = findAccount(publicKey);
        if (account == null) throw new AccountDoesNotExistException();
        else return account;
    }

    public void openAccount(PublicKey publicKey) {
        accounts.putIfAbsent(publicKey, new Account(publicKey));
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

    // Remove later:
    public void incrementBalance(PublicKey publicKey, int amount) throws AccountDoesNotExistException, AmountTooLowException {
        if (amount <= 0) throw new AmountTooLowException();
        Account account = findAccount(publicKey);
        if (account == null) throw new AccountDoesNotExistException();
        account.incrementBalance(amount);
    }

    // Remove later:
    public void decrementBalance(PublicKey publicKey, int amount) throws AccountDoesNotExistException, BalanceTooLowException, AmountTooLowException {
        if (amount <= 0) throw new AmountTooLowException();
        Account account = findAccount(publicKey);
        if (account == null) throw new AccountDoesNotExistException();
        if (!account.canDecrement(amount)) throw new BalanceTooLowException();
        account.decrementBalance(amount);
    }

    // Send Amount:

    public void sendAmount(PublicKey sourceKey, PublicKey destinationKey, int amount) throws AccountDoesNotExistException, AmountTooLowException, BalanceTooLowException {
        if (amount <= 0) throw new AmountTooLowException();
        Account source = findAccount(sourceKey);
        if (source == null) throw new AccountDoesNotExistException();
        Account destination = findAccount(destinationKey);
        if (destination == null) throw new AccountDoesNotExistException();
        if (!source.canDecrement(amount)) throw new BalanceTooLowException();
        // Should we decrement from the source balance here?
        // Or wait for it to be approved by the receiver and only then decrement and increment at once
        // Second approach requires doing the source.canDecrement(amount) check again later (what is implemented for now)
        destination.addPendingTransfer(sourceKey, amount);
    }

    // Receive Amount:

    public void receiveAmount(PublicKey publicKey, int transferNum)
            throws AccountDoesNotExistException, BalanceTooLowException, InvalidRequestException {
        Account destination = findAccount(publicKey);
        if (destination == null) throw new AccountDoesNotExistException();
        if (transferNum < 0 || transferNum >= destination.getPendingTransfers().length()) throw new InvalidRequestException();
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
}
