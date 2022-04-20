package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.grpc.Server.*;
import pt.tecnico.sec.bftb.server.exceptions.AccountDoesNotHavePuzzleException;
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
	public static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
	private static final int PUZZLE_SALT_LENGTH = 10;
	private static final long PUZZLE_SEARCH_RANGE = 100000;
	private final Random randomGenerator;
	private final PrivateKey privateKey;
	private final Map<PublicKey, Long> currentNonces;
	private final Map<ByteString, Long> currentPuzzleSolutions;


	public SignatureManager(int replicaID) throws PrivateKeyLoadingFailedException {
		this.randomGenerator = new SecureRandom();
		this.privateKey = Resources.getPrivateKey(replicaID);
		this.currentNonces = new ConcurrentHashMap<>();
		this.currentPuzzleSolutions = new ConcurrentHashMap<>();
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

	public boolean isNonceSignatureInvalid(PublicKey peerPublicKey, byte[] signature, byte[] content) throws
			SignatureVerificationFailedException {
		try {
			if (!currentNonces.containsKey(peerPublicKey))
				throw new SignatureVerificationFailedException("Account does not have a currently usable nonce");
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
				return false;
			}
			else return true;
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | BufferUnderflowException e) {
			throw new SignatureVerificationFailedException(e);
		}
	}

	public boolean isNonceSignatureInvalid(PublicKey peerPublicKey, byte[] signature) throws
			SignatureVerificationFailedException {
		return isNonceSignatureInvalid(peerPublicKey, signature, new byte[0]);
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
		catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | BufferUnderflowException e) {
			throw new SignatureVerificationFailedException(e);
		}
	}

	public boolean isBalanceSignatureValid(PublicKey publicKey, byte[] signature, Balance balance) throws
			SignatureVerificationFailedException {
		return isSignatureValid(publicKey, signature, balance.toByteArray());
	}

	public boolean isTransferSignatureValid(PublicKey publicKey, byte[] signature, Transfer transfer) throws
			SignatureVerificationFailedException {
		return isSignatureValid(publicKey, signature, transfer.toByteArray());
	}

	public boolean isListSizesSignatureValid(PublicKey publicKey, byte[] signature, ListSizes listSizes) throws
			SignatureVerificationFailedException {
		return isSignatureValid(publicKey, signature, listSizes.toByteArray());
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

	public Puzzle generatePuzzle(ByteString peerPublicKeyBS) throws NoSuchAlgorithmException {
		// Remove old nonce if it exists
		currentPuzzleSolutions.remove(peerPublicKeyBS);
		// Generate new nonce, store it and return it
		long puzzleSolution = randomGenerator.nextLong(0, PUZZLE_SEARCH_RANGE);
		currentPuzzleSolutions.put(peerPublicKeyBS, puzzleSolution);

		byte[] puzzleSalt = new byte[PUZZLE_SALT_LENGTH];
		randomGenerator.nextBytes(puzzleSalt);

		byte[] puzzle = createPuzzle(puzzleSolution, puzzleSalt);
		return Puzzle.newBuilder()
				.setPuzzle(ByteString.copyFrom(puzzle))
				.setPuzzleSalt(ByteString.copyFrom(puzzleSalt))
				.build();
	}

	public byte[] createPuzzle(long solution, byte[] salt) throws NoSuchAlgorithmException {
		byte[] rawPuzzle = ByteBuffer.allocate(Long.BYTES + salt.length).putLong(solution).put(salt).array();
		return MessageDigest.getInstance("SHA-256").digest(rawPuzzle);
	}

	public boolean isPuzzleSolutionCorrect(ByteString peerPublicKeyBS, long solution)
			throws AccountDoesNotHavePuzzleException {
		if (!currentPuzzleSolutions.containsKey(peerPublicKeyBS)) {
			throw new AccountDoesNotHavePuzzleException();
		}
		long currentSolution = currentPuzzleSolutions.get(peerPublicKeyBS);
		System.out.println("Current solution: " + currentSolution + ", received solution: " + solution);
		return currentSolution == solution;
	}
}
