package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.AccountDoesNotExistException;
import pt.tecnico.sec.bftb.server.exceptions.BalanceTooLowException;

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

    public void createAccount(PublicKey publicKey, int balance) throws BalanceTooLowException {
        if (balance < 0) throw new BalanceTooLowException();
        accounts.putIfAbsent(publicKey, new Account(publicKey, balance));
    }
}
