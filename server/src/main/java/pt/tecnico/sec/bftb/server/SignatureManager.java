package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.CypherFailedException;
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
	private final Random randomGenerator;
	private final PrivateKey privateKey;
	private final Map<PublicKey, Long> currentNonces;


	public SignatureManager() {
		this.randomGenerator = new SecureRandom();
		this.privateKey = Resources.getPrivateKey();
		this.currentNonces = new ConcurrentHashMap<>();
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

	public boolean verifySignature(PublicKey peerPublicKey, byte[] content, byte[] signature) throws
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
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
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

	public byte[] sign(long nonce, byte[] content) {
		try {
			// Concatenate nonce and content (which might be empty)
			byte[] request = ByteBuffer.allocate(Long.BYTES + content.length).putLong(nonce).put(content).array();
			// Hash it with SHA-256
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(request);
			// Encrypt SERVER's signature
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, this.privateKey);
			return cipher.doFinal(hash);
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
