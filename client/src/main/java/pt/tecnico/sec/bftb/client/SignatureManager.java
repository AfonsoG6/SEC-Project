package pt.tecnico.sec.bftb.client;

import pt.tecnico.sec.bftb.client.exceptions.CypherFailedException;
import pt.tecnico.sec.bftb.client.exceptions.SignatureVerificationFailedException;

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
	private long currentNonce;

	public SignatureManager(PrivateKey privateKey) {
		this.randomGenerator = new SecureRandom();
		this.privateKey = privateKey;
		this.currentNonce = 0;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
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
		// Generate new nonce, store it and return it
		this.currentNonce = randomGenerator.nextLong();
		return cypherNonce(peerPublicKey, currentNonce);
	}

	public boolean isSignatureInvalid(PublicKey peerPublicKey, byte[] signature, byte[] content) throws
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
			return !Arrays.equals(expectedHash, receivedHash);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | BufferUnderflowException e) {
			throw new SignatureVerificationFailedException(e);
		}
	}

	public boolean isSignatureInvalid(PublicKey peerPublicKey, byte[] signature) throws
			SignatureVerificationFailedException {
		return isSignatureInvalid(peerPublicKey, signature, new byte[0]);
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
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new CypherFailedException(e);
		}
	}

	public byte[] sign(long nonce) throws CypherFailedException {
		return sign(nonce, new byte[0]);
	}
}