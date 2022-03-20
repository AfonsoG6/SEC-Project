package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final Random randomGenerator;
    private final ConcurrentHashMap<PublicKey, Account> accounts;
    private final Map<PublicKey, Long> currentNonces = new ConcurrentHashMap<>();

    public Server() throws NoSuchAlgorithmException {
        this.randomGenerator = SecureRandom.getInstanceStrong();
        this.accounts = new ConcurrentHashMap<>();
    }

    private byte[] cypherNonce(PublicKey publicKey, long nonce) throws CypherFailedException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] nonceBytes = ByteBuffer.allocate(Long.BYTES).putLong(nonce).array();
            return cipher.doFinal(nonceBytes);
        }
        catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new CypherFailedException(e);
        }
    }

    public byte[] generateNonce(PublicKey publicKey) throws CypherFailedException {
        // Remove old nonce if it exists
        currentNonces.remove(publicKey);
        // Generate new nonce, store it and return it
        long nonce = randomGenerator.nextLong();
        currentNonces.put(publicKey, nonce);
        return cypherNonce(publicKey, nonce);
    }

    public boolean verifySignature(PublicKey publicKey, byte[] signature) throws SignatureVerificationFailedException {
        try {
            if (!currentNonces.containsKey(publicKey)) throw new SignatureVerificationFailedException("Account does not have a currently usable nonce");
            long usableNonce = currentNonces.get(publicKey);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] nonceBytes = cipher.doFinal(signature);
            long nonce = ByteBuffer.wrap(nonceBytes).getLong();
            if (nonce == usableNonce) {
                currentNonces.remove(publicKey);
                return true;
            }
            else return false;
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | BufferUnderflowException e) {
            throw new SignatureVerificationFailedException(e);
        }
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
        if (!destination.isPendingTransferNumValid(transferNum)) throw new InvalidRequestException("Invalid transfer number");
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
