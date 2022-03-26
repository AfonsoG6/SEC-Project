package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.CypherFailedException;
import pt.tecnico.sec.bftb.server.exceptions.PrivateKeyLoadingFailedException;
import pt.tecnico.sec.bftb.server.exceptions.SignatureVerificationFailedException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class SignatureManager {
	public static final String CIPHER_TRANSFORMATION = "RSA/CBC/PKCS1Padding";
	private final Random randomGenerator;
	private final PrivateKey privateKey;
	private final Map<PublicKey, Long> currentNonces;


	public SignatureManager() throws PrivateKeyLoadingFailedException {
		this.randomGenerator = new SecureRandom();
		this.privateKey = Resources.getPrivateKey();
		this.currentNonces = new ConcurrentHashMap<>();
	}

	public byte[] cypherNonce(PublicKey peerPublicKey, long nonce) throws CypherFailedException {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, peerPublicKey);
			byte[] nonceBytes = ByteBuffer.allocate(Long.BYTES).putLong(nonce).array();
			return cipher.doFinal(nonceBytes);
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
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
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new CypherFailedException(e);
		}
	}

	public byte[] generateCypheredNonce(PublicKey peerPublicKey) throws CypherFailedException {
		// Remove old nonce if it exists
		currentNonces.remove(peerPublicKey);
		// Generate new nonce, store it and return it
		long nonce = randomGenerator.nextLong();
		currentNonces.put(peerPublicKey, nonce);
		return cypherNonce(peerPublicKey, nonce);
	}

	public boolean verifySignature(PublicKey peerPublicKey, byte[] signature, byte[] content) throws
			SignatureVerificationFailedException {
		try {
			if (!currentNonces.containsKey(peerPublicKey)) throw new SignatureVerificationFailedException("Account does not have a currently usable nonce");
			// Get nonce
			long nonce = currentNonces.get(peerPublicKey);
			// Concatenate nonce and content
			byte[] request = ByteBuffer.allocate(Long.BYTES + content.length).putLong(nonce).put(content).array();
			// Hash it with SHA-256
			byte[] expectedHash = MessageDigest.getInstance("SHA-256").digest(request);
			// Decrypt CLIENT's signature
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, peerPublicKey);
			byte[] receivedHash = cipher.doFinal(signature);
			// Compare the received hash with the expected one
			if (Arrays.equals(expectedHash, receivedHash)) {
				currentNonces.remove(peerPublicKey);
				return true;
			}
			else return false;
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | BufferUnderflowException e) {
			throw new SignatureVerificationFailedException(e);
		}
	}

	public boolean verifySignature(PublicKey peerPublicKey, byte[] signature) throws
			SignatureVerificationFailedException {
		return verifySignature(peerPublicKey, signature, new byte[0]);
	}

	public byte[] sign(long nonce, byte[] content) throws CypherFailedException {
		try {
			// Concatenate nonce and content (which might be empty)
			byte[] request = ByteBuffer.allocate(Long.BYTES + content.length).putLong(nonce).put(content).array();
			// Hash it with SHA-256
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(request);
			// Encrypt SERVER's signature
			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, this.privateKey);
			return cipher.doFinal(hash);
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new CypherFailedException(e);
		}
	}

	public byte[] sign(long nonce) throws CypherFailedException {
		return sign(nonce, new byte[0]);
	}
}
