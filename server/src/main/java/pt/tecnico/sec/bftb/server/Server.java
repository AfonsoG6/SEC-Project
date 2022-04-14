package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.server.exceptions.*;
import pt.tecnico.sec.bftb.grpc.Server.Transfer;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;
import java.util.List;

public class Server {
	private static final long TIMESTAMP_TOLERANCE = 5000;
	private static final int INITIAL_BALANCE = 100;
	private final SignatureManager signatureManager;
	SQLiteDatabase db;
	int replicaID;

	public Server(int replicaID) throws ServerInitializationFailedException {
		try {
			Resources.init();
			this.replicaID = replicaID;
			this.signatureManager = new SignatureManager(replicaID);
			this.db = new SQLiteDatabase(replicaID);
		}
		catch (PrivateKeyLoadingFailedException | DirectoryCreationFailedException | SQLException e) {
			throw new ServerInitializationFailedException(e);
		}
	}

	public SignatureManager getSignatureManager() {
		return signatureManager;
	}

	public void openAccount(ByteString publicKey, Balance balance, ByteString balanceSignature)
			throws AccountAlreadyExistsException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException,
			InvalidNewBalanceException, SignatureVerificationFailedException {
		if (db.checkAccountExists(publicKey)) throw new AccountAlreadyExistsException();
		verifyInitialBalance(publicKey, balance, balanceSignature);

		db.insertAccount(publicKey, INITIAL_BALANCE);
	}

	// Check Account (part 1):

	public int getAccountBalance(ByteString publicKey) throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKey)) throw new AccountDoesNotExistException();
		return db.readAccountBalance(publicKey);
	}

	// Check Account (part 2):

	public List<Transfer> getPendingIncomingTransfers(ByteString publicKey)
			throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKey)) throw new AccountDoesNotExistException();
		return db.getIncomingPendingTransfersOfAccount(publicKey);
	}

	// Send Amount:

	public void sendAmount(Transfer transfer, ByteString senderSignature)
			throws AccountDoesNotExistException, AmountTooLowException, BalanceTooLowException, SQLException, InvalidTimestampException {
		long timestamp = transfer.getTimestamp();
		int amount = transfer.getAmount();
		ByteString senderKeyBS = transfer.getSenderKey();
		ByteString receiverKeyBS = transfer.getReceiverKey();

		verifySendAmount(timestamp, amount, senderKeyBS, receiverKeyBS);
		verifyTransferSignature(senderKeyBS, transfer, senderSignature);
		verifyNewBalance(senderKeyBS, newBalance, balanceSignature, -amount);

		// TRANSACTION
		db.insertTransfer(timestamp, senderKeyBS, receiverKeyBS, amount, senderSignature);
		db.writeAccountBalance(senderKeyBS, db.readAccountBalance(senderKeyBS) - transfer.getAmount());
	}

	private void verifyTransferSignature(ByteString publicKeyBS, Transfer transfer, ByteString signature)
			throws SignatureVerificationFailedException, InvalidTransferSignatureException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		PublicKey publicKey = publicKeyFromByteString(publicKeyBS);
		if (this.signatureManager.isTransferSignatureValid(publicKey, signature.toByteArray(), transfer)) {
			throw new InvalidTransferSignatureException();
		}
	}

	private PublicKey publicKeyFromByteString(ByteString publicKeyBS) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBS.toByteArray()));
	}

	private void verifyInitialBalance(ByteString userPublicKeyBS, Balance initialBalance, ByteString signature)
			throws InvalidNewBalanceException, NoSuchAlgorithmException, SignatureVerificationFailedException,
			InvalidKeySpecException {
		if (initialBalance.getValue() != INITIAL_BALANCE || initialBalance.getWts() != 0) {
			throw new InvalidNewBalanceException();
		}
		PublicKey userPublicKey = publicKeyFromByteString(userPublicKeyBS);
		if (this.signatureManager.isBalanceSignatureValid(userPublicKey, signature.toByteArray(), initialBalance)) {
			throw new InvalidNewBalanceException("Balance signature does not match received balance");
		}
	}

	private void verifyNewBalance(ByteString userPublicKeyBS, Balance newBalance, ByteString signature, int expectedDiff)
			throws SignatureVerificationFailedException, InvalidNewBalanceException, SQLException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		BalanceRecord balanceRecord = db.readAccountBalanceRecord(userPublicKeyBS);
		if (newBalance.getValue() != balanceRecord.getBalance().getValue() + expectedDiff) {
			throw new InvalidNewBalanceException("New balance value does not match expected value");
		}
		if (newBalance.getWts() != balanceRecord.getBalance().getWts() + 1) {
			throw new InvalidNewBalanceException("New balance timestamp does match expected timestamp");
		}
		PublicKey userPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(userPublicKeyBS.toByteArray()));
		if (this.signatureManager.isBalanceSignatureValid(userPublicKey, signature.toByteArray(), newBalance)) {
			throw new InvalidNewBalanceException("Balance signature does not match received balance");
		}
	}

	private void verifySendAmount(long timestamp, int amount, ByteString senderKeyBS, ByteString receiverKeyBS)
			throws AmountTooLowException, InvalidTimestampException, SQLException, AccountDoesNotExistException,
			BalanceTooLowException {
		if (amount <= 0) throw new AmountTooLowException();
		long currentTime = System.currentTimeMillis();
		if (timestamp > currentTime || timestamp < currentTime - TIMESTAMP_TOLERANCE) throw new InvalidTimestampException();
		if (!db.checkAccountExists(senderKeyBS)) throw new AccountDoesNotExistException();
		if (!db.checkAccountExists(receiverKeyBS)) throw new AccountDoesNotExistException();
		if (amount >= db.readAccountBalance(senderKeyBS)) throw new BalanceTooLowException();
	}

	public void receiveAmount(Transfer transfer, ByteString receiverSignature, Balance balance, ByteString balanceSignature)
			throws TransferNotFoundException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException,
			InvalidNewBalanceException, SignatureVerificationFailedException, InvalidTransferSignatureException {
		long timestamp = transfer.getTimestamp();
		ByteString senderKeyBS = transfer.getSenderKey();
		ByteString receiverKeyBS = transfer.getReceiverKey();
		int amount = transfer.getAmount();

		verifyReceiveAmount(timestamp, senderKeyBS, receiverKeyBS, amount);
		verifyTransferSignature(senderKeyBS, transfer, receiverSignature);
		verifyNewBalance(senderKeyBS, balance, balanceSignature, +amount);

		// TRANSACTION
		db.updateTransferToApproved(timestamp, senderKeyBS, receiverKeyBS, receiverSignature);
		db.writeAccountBalance(receiverKeyBS, db.readAccountBalance(receiverKeyBS) + transfer.getAmount());
	}

	private void verifyReceiveAmount(long timestamp, ByteString senderKeyBS, ByteString receiverKeyBS, int amount)
			throws SQLException, TransferNotFoundException {
		if (!db.checkPendingTransferExists(timestamp, senderKeyBS, receiverKeyBS, amount)) throw new TransferNotFoundException();
	}

	public List<Transfer> getApprovedTransfers(ByteString publicKey) throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKey)) throw new AccountDoesNotExistException();
		return db.getAllTransfersOfAccount(publicKey);
	}
}
