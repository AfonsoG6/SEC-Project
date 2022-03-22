package pt.tecnico.sec.bftb.client;

import pt.tecnico.sec.bftb.client.exceptions.SignatureVerificationFailedException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;

public class SignatureManager {
	private PrivateKey privateKey;

	public SignatureManager(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public boolean verifySignature(PublicKey peerPublicKey, long nonce, byte[] content, byte[] signature) throws
			SignatureVerificationFailedException {
		try {
			// Concatenate nonce and content
			byte[] request = ByteBuffer.allocate(Long.BYTES + content.length).putLong(nonce).put(content).array();
			// Hash it with SHA-256
			byte[] expectedHash = MessageDigest.getInstance("SHA-256").digest(request);
			// Decrypt SERVER's signature
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, peerPublicKey);
			byte[] receivedHash = cipher.doFinal(signature);
			// Compare the received hash with the expected one
			return Arrays.equals(expectedHash, receivedHash);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | BufferUnderflowException e) {
			throw new SignatureVerificationFailedException(e);
		}
	}

	public byte[] sign(long nonce, byte[] content) {
		try {
			// Concatenate nonce and content
			byte[] request = ByteBuffer.allocate(Long.BYTES + content.length).putLong(nonce).put(content).array();
			// Hash it with SHA-256
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(request);
			// Encrypt CLIENT's signature
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
