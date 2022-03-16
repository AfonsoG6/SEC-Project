package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.AccountDoesNotExistException;
import pt.tecnico.sec.bftb.server.exceptions.BalanceTooLowException;
import pt.tecnico.sec.bftb.server.exceptions.AmountTooLowException;

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

    public void createAccount(PublicKey publicKey) {
        accounts.putIfAbsent(publicKey, new Account(publicKey));
    }

    public void incrementBalance(PublicKey publicKey, int value) throws AccountDoesNotExistException, AmountTooLowException {
        if (value <= 0) throw new AmountTooLowException();
        Account account = findAccount(publicKey);
        if (account == null) throw new AccountDoesNotExistException();
        account.incrementBalance(value);
    }

    public void decrementBalance(PublicKey publicKey, int value) throws AccountDoesNotExistException, BalanceTooLowException, AmountTooLowException {
        if (value <= 0) throw new AmountTooLowException();
        Account account = findAccount(publicKey);
        if (account == null) throw new AccountDoesNotExistException();
        if (!account.canDecrement(value)) throw new BalanceTooLowException();
        account.decrementBalance(value);
    }
}
