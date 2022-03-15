package pt.tecnico.sec.server;

import pt.tecnico.sec.server.exceptions.AccountDoesNotExistException;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final ConcurrentHashMap<String, Account> accounts;

    public Server() {
        accounts = new ConcurrentHashMap<>();
    }

    public Account findAccount(String name) {
        return accounts.get(name);
    }

    public Account getAccount(String name) throws AccountDoesNotExistException {
        Account account = findAccount(name);
        if (account == null) throw new AccountDoesNotExistException();
        else return account;
    }

    public void createAccount(String name, PublicKey publicKey) {
        accounts.putIfAbsent(name, new Account(name, publicKey));
    }
}
