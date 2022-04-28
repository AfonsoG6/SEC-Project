package pt.tecnico.sec.bftb.client;

import pt.tecnico.sec.bftb.client.exceptions.CypherFailedException;
import pt.tecnico.sec.bftb.client.exceptions.SignatureVerificationFailedException;
import pt.tecnico.sec.bftb.grpc.Server.Balance;
import pt.tecnico.sec.bftb.grpc.Server.ListSizes;
import pt.tecnico.sec.bftb.grpc.Server.Transfer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;
import java.util.Random;

public class SignatureManager {
	public static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
	private final Random randomGenerator;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private long currentNonce;

	public SignatureManager(PrivateKey privateKey, PublicKey publicKey) {
		this.randomGenerator = new SecureRandom();
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.currentNonce = 0;
	}

	public void setKeyPair(PrivateKey privateKey, PublicKey publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	public byte[] cypherNonce(PublicKey peerPublicKey, long nonce) throws CypherFailedException {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, peerPublicKey);
			byte[] nonceBytes = ByteBuffer.allocate(Long.BYTES).putLong(nonce).array();
			return cipher.doFinal(nonceBytes);
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException |
		       NoSuchPaddingException e) {
			throw new CypherFailedException(e);
		}
	}

	public long decypherNonce(byte[] cypheredNonce) throws CypherFailedException {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
			byte[] nonceBytes = cipher.doFinal(cypheredNonce);
			return ByteBuffer.wrap(nonceBytes).getLong();
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException |
		       NoSuchPaddingException e) {
			throw new CypherFailedException(e);
		}
	}

	public long generateNonce() {
		// Generate new nonce, store it and return it
		this.currentNonce = randomGenerator.nextLong();
		return this.currentNonce;
	}

	public boolean isNonceSignatureValid(PublicKey peerPublicKey, byte[] signature, byte[] content) throws
			SignatureVerificationFailedException {
		try {
			// Concatenate nonce and content
			byte[] request = ByteBuffer.allocate(Long.BYTES + content.length).putLong(this.currentNonce).put(content).array();
			// Hash it with SHA-256
			byte[] expectedHash = MessageDigest.getInstance("SHA-256").digest(request);
			// Decrypt SERVER's signature
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, peerPublicKey);
			byte[] receivedHash = cipher.doFinal(signature);
			// Compare the received hash with the expected one
			return Arrays.equals(expectedHash, receivedHash);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
		       NoSuchPaddingException | BufferUnderflowException e) {
			throw new SignatureVerificationFailedException(e);
		}
	}

	public boolean isNonceSignatureValid(PublicKey peerPublicKey, byte[] signature) throws
			SignatureVerificationFailedException {
		return isNonceSignatureValid(peerPublicKey, signature, new byte[0]);
	}

	public boolean isBalanceSignatureValid(byte[] signature, Balance balance) throws
			SignatureVerificationFailedException {
		return isSignatureValid(this.publicKey, signature, balance.toByteArray());
	}

	public boolean isTransferSignatureValid(PublicKey peerPublicKey, byte[] signature, Transfer transfer) throws
			SignatureVerificationFailedException {
		return isSignatureValid(peerPublicKey, signature, transfer.toByteArray());
	}

	public boolean isTransferSignatureValid(byte[] signature, Transfer transfer) throws
			SignatureVerificationFailedException {
		return isSignatureValid(this.publicKey, signature, transfer.toByteArray());
	}

	public boolean isListSizesSignatureValid(byte[] signature, ListSizes listSizes)
			throws SignatureVerificationFailedException {
		return isSignatureValid(this.publicKey, signature, listSizes.toByteArray());
	}

	public boolean isListSizesSignatureValid(PublicKey signerPublicKey, byte[] signature, ListSizes listSizes)
			throws SignatureVerificationFailedException {
		return isSignatureValid(signerPublicKey, signature, listSizes.toByteArray());
	}

	public boolean isSignatureValid(PublicKey peerPublicKey, byte[] signature, byte[] content) throws
			SignatureVerificationFailedException {
		try {
			// Hash it with SHA-256
			byte[] expectedHash = MessageDigest.getInstance("SHA-256").digest(content);
			// Decrypt SERVER's signature
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, peerPublicKey);
			byte[] receivedHash = cipher.doFinal(signature);
			// Compare the received hash with the expected one
			return Arrays.equals(expectedHash, receivedHash);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
		       NoSuchPaddingException | BufferUnderflowException e) {
			throw new SignatureVerificationFailedException(e);
		}
	}

	public byte[] signBalance(Balance balance) throws CypherFailedException {
		return sign(balance.toByteArray());
	}

	public byte[] signTransfer(Transfer transfer) throws CypherFailedException {
		return sign(transfer.toByteArray());
	}

	public byte[] signListSizes(ListSizes listSizes) throws CypherFailedException {
		return sign(listSizes.toByteArray());
	}

	public byte[] sign(byte[] content) throws CypherFailedException {
		try {
			// Hash it with SHA-256
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
			// Encrypt CLIENT's signature
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, this.privateKey);
			return cipher.doFinal(hash);
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException |
		       NoSuchPaddingException e) {
			throw new CypherFailedException(e);
		}
	}

	public byte[] sign(long nonce, byte[] content) throws CypherFailedException {
		try {
			// Concatenate nonce and content
			byte[] request = ByteBuffer.allocate(Long.BYTES + content.length).putLong(nonce).put(content).array();
			// Hash it with SHA-256
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(request);
			// Encrypt CLIENT's signature
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, this.privateKey);
			return cipher.doFinal(hash);
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException |
		       NoSuchPaddingException e) {
			throw new CypherFailedException(e);
		}
	}

	public byte[] sign(long nonce) throws CypherFailedException {
		return sign(nonce, new byte[0]);
	}
}
