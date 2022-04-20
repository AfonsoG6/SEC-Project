package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.server.exceptions.*;
import pt.tecnico.sec.bftb.grpc.Server.*;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;

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

	private void verifyInitialBalance(ByteString userPublicKeyBS, Balance initialBalance, ByteString signature)
			throws InvalidNewBalanceException, NoSuchAlgorithmException, SignatureVerificationFailedException,
			InvalidKeySpecException {
		if (initialBalance.getValue() != INITIAL_BALANCE || initialBalance.getWts() != 0) {
			throw new InvalidNewBalanceException();
		}
		PublicKey userPublicKey = publicKeyFromByteString(userPublicKeyBS);
		if (!this.signatureManager.isBalanceSignatureValid(userPublicKey, signature.toByteArray(), initialBalance)) {
			throw new InvalidNewBalanceException("Balance signature does not match received balance");
		}
	}

	private void verifyInitialListSizes(ByteString userPublicKeyBS, ListSizes initialListSizes, ByteString signature)
			throws NoSuchAlgorithmException, SignatureVerificationFailedException,
			InvalidKeySpecException, InvalidNewListSizesException {
		if (initialListSizes.getPendingSize() != 0 || initialListSizes.getApprovedSize() != 0 || initialListSizes.getWts() != 0) {
			throw new InvalidNewListSizesException();
		}
		PublicKey userPublicKey = publicKeyFromByteString(userPublicKeyBS);
		if (!this.signatureManager.isListSizesSignatureValid(userPublicKey, signature.toByteArray(), initialListSizes)) {
			throw new InvalidNewListSizesException("ListSizes signature does not match received listSizes");
		}
	}

	public void openAccount(ByteString publicKeyBS, Balance balance, ByteString balanceSignature, ListSizes listSizes, ByteString sizesSignature)
			throws AccountAlreadyExistsException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException,
			InvalidNewBalanceException, SignatureVerificationFailedException, InvalidNewListSizesException {
		if (db.checkAccountExists(publicKeyBS)) throw new AccountAlreadyExistsException();
		verifyInitialBalance(publicKeyBS, balance, balanceSignature);
		verifyInitialListSizes(publicKeyBS, listSizes, sizesSignature);

		db.insertAccount(publicKeyBS, INITIAL_BALANCE, balance.getWts(), balanceSignature, listSizes.getPendingSize(),
				listSizes.getApprovedSize(), listSizes.getWts(), sizesSignature);
	}

	public BalanceRecord readBalance(ByteString publicKeyBS) throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKeyBS)) throw new AccountDoesNotExistException();
		return db.readAccountBalanceRecord(publicKeyBS);
	}

	public ListSizesRecord readListSizes(ByteString publicKeyBS)
			throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKeyBS)) throw new AccountDoesNotExistException();
		return db.readAccountListSizesRecord(publicKeyBS);
	}

	public TransfersRecord getPendingIncomingTransfers(ByteString publicKeyBS)
			throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKeyBS)) throw new AccountDoesNotExistException();
		return db.getIncomingPendingTransfersOfAccount(publicKeyBS);
	}

	public void sendAmount(Transfer transfer, ByteString senderSignature, Balance newBalance, ByteString balanceSignature,
			ListSizes receiverListSizes, ByteString receiverSizesSignature)
			throws AccountDoesNotExistException, AmountTooLowException, BalanceTooLowException, SQLException,
			InvalidTimestampException, InvalidKeySpecException,
			SignatureVerificationFailedException, InvalidTransferSignatureException, InvalidNewBalanceException,
			NoSuchAlgorithmException, InvalidNewListSizesException {
		long timestamp = transfer.getTimestamp();
		int amount = transfer.getAmount();
		ByteString senderKeyBS = transfer.getSenderKey();
		ByteString receiverKeyBS = transfer.getReceiverKey();

		verifySendAmount(timestamp, amount, senderKeyBS, receiverKeyBS);
		verifyTransferSignature(senderKeyBS, transfer, senderSignature);
		verifyNewBalance(senderKeyBS, newBalance, balanceSignature, -amount);
		verifyNewListSizesNewPending(receiverKeyBS, receiverListSizes, receiverSizesSignature, senderKeyBS);

		// TRANSACTION
		db.insertTransfer(timestamp, senderKeyBS, receiverKeyBS, amount, senderSignature);
		db.updateAccountBalance(senderKeyBS, newBalance.getValue(), newBalance.getWts(), balanceSignature);
		db.updateAccountListSizes(receiverKeyBS, receiverListSizes.getPendingSize(), receiverListSizes.getApprovedSize(), receiverListSizes.getWts(), receiverSizesSignature, senderKeyBS);
	}

	private void verifyTransferSignature(ByteString publicKeyBS, Transfer transfer, ByteString signature)
			throws SignatureVerificationFailedException, InvalidTransferSignatureException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		PublicKey publicKey = publicKeyFromByteString(publicKeyBS);
		if (!this.signatureManager.isTransferSignatureValid(publicKey, signature.toByteArray(), transfer)) {
			throw new InvalidTransferSignatureException();
		}
	}

	private PublicKey publicKeyFromByteString(ByteString publicKeyBS) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBS.toByteArray()));
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
		PublicKey userPublicKey = publicKeyFromByteString(userPublicKeyBS);
		if (!this.signatureManager.isBalanceSignatureValid(userPublicKey, signature.toByteArray(), newBalance)) {
			throw new InvalidNewBalanceException("Balance signature does not match received balance");
		}
	}

	private void verifyNewListSizesNewPending(ByteString publicKeyBS, ListSizes newListSizes, ByteString sizesSignature, ByteString signerPublicKeyBS)
			throws SignatureVerificationFailedException, SQLException,
			NoSuchAlgorithmException, InvalidKeySpecException, InvalidNewListSizesException {
		ListSizesRecord listSizesRecord = db.readAccountListSizesRecord(publicKeyBS);
		if (newListSizes.getPendingSize() != listSizesRecord.getListSizes().getPendingSize() + 1) {
			throw new InvalidNewListSizesException("New pending transfers list size value does not match expected value");
		}
		if (newListSizes.getApprovedSize() != listSizesRecord.getListSizes().getApprovedSize()) {
			throw new InvalidNewListSizesException("New approved transfers list size value does not match expected value");
		}
		if (newListSizes.getWts() != listSizesRecord.getListSizes().getWts() + 1) {
			throw new InvalidNewListSizesException("New list sizes timestamp does not match expected timestamp");
		}
		PublicKey signerPublicKey = publicKeyFromByteString(signerPublicKeyBS);
		if (!this.signatureManager.isListSizesSignatureValid(signerPublicKey, sizesSignature.toByteArray(), newListSizes)) {
			throw new InvalidNewListSizesException("List sizes signature does not match received list sizes");
		}
	}

	private void verifyNewListSizesNewApproved(ByteString publicKeyBS, ListSizes newListSizes, ByteString sizesSignature, ByteString signerPublicKeyBS)
			throws SignatureVerificationFailedException, SQLException,
			NoSuchAlgorithmException, InvalidKeySpecException, InvalidNewListSizesException {
		ListSizesRecord listSizesRecord = db.readAccountListSizesRecord(publicKeyBS);
		if (newListSizes.getPendingSize() != listSizesRecord.getListSizes().getPendingSize()) {
			throw new InvalidNewListSizesException("New pending transfers list size value does not match expected value");
		}
		if (newListSizes.getApprovedSize() != listSizesRecord.getListSizes().getApprovedSize() + 1) {
			throw new InvalidNewListSizesException("New approved transfers list size value does not match expected value");
		}
		if (newListSizes.getWts() != listSizesRecord.getListSizes().getWts() + 1) {
			throw new InvalidNewListSizesException("New list sizes timestamp does not match expected timestamp");
		}
		PublicKey signerPublicKey = publicKeyFromByteString(signerPublicKeyBS);
		if (!this.signatureManager.isListSizesSignatureValid(signerPublicKey, sizesSignature.toByteArray(), newListSizes)) {
			throw new InvalidNewListSizesException("List sizes signature does not match received list sizes");
		}
	}

	private void verifyNewListSizesPendingToApproved(ByteString publicKeyBS, ListSizes newListSizes, ByteString sizesSignature, ByteString signerPublicKeyBS)
			throws SignatureVerificationFailedException, SQLException,
			NoSuchAlgorithmException, InvalidKeySpecException, InvalidNewListSizesException {
		ListSizesRecord listSizesRecord = db.readAccountListSizesRecord(publicKeyBS);
		if (newListSizes.getPendingSize() != listSizesRecord.getListSizes().getPendingSize() - 1) {
			throw new InvalidNewListSizesException("New pending transfers list size value does not match expected value");
		}
		if (newListSizes.getApprovedSize() != listSizesRecord.getListSizes().getApprovedSize() + 1) {
			throw new InvalidNewListSizesException("New pending transfers list size value does not match expected value");
		}
		if (newListSizes.getWts() != listSizesRecord.getListSizes().getWts() + 1) {
			throw new InvalidNewListSizesException("New list sizes timestamp does not match expected timestamp");
		}
		PublicKey signerPublicKey = publicKeyFromByteString(signerPublicKeyBS);
		if (!this.signatureManager.isListSizesSignatureValid(signerPublicKey, sizesSignature.toByteArray(), newListSizes)) {
			throw new InvalidNewListSizesException("List sizes signature does not match received list sizes");
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

	public void receiveAmount(Transfer transfer, ByteString receiverSignature, Balance balance, ByteString balanceSignature,
			ListSizes senderListSizes, ByteString senderSizesSignature, ListSizes receiverListSizes, ByteString receiverSizesSignature)
			throws TransferNotFoundException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException,
			InvalidNewBalanceException, SignatureVerificationFailedException, InvalidTransferSignatureException,
			InvalidNewListSizesException {
		long timestamp = transfer.getTimestamp();
		ByteString senderKeyBS = transfer.getSenderKey();
		ByteString receiverKeyBS = transfer.getReceiverKey();
		int amount = transfer.getAmount();

		verifyReceiveAmount(timestamp, senderKeyBS, receiverKeyBS, amount);
		verifyTransferSignature(receiverKeyBS, transfer, receiverSignature);
		verifyNewBalance(receiverKeyBS, balance, balanceSignature, +amount);
		verifyNewListSizesNewApproved(senderKeyBS, senderListSizes, senderSizesSignature, receiverKeyBS);
		verifyNewListSizesPendingToApproved(receiverKeyBS, receiverListSizes, receiverSizesSignature, receiverKeyBS);

		// TRANSACTION
		db.updateTransferToApproved(timestamp, senderKeyBS, receiverKeyBS, receiverSignature);
		db.updateAccountAll(receiverKeyBS, balance.getValue(), balance.getWts(), balanceSignature, receiverListSizes.getPendingSize(),
				receiverListSizes.getApprovedSize(), receiverListSizes.getWts(), receiverSizesSignature, receiverKeyBS);
		db.updateAccountListSizes(senderKeyBS, senderListSizes.getPendingSize(), senderListSizes.getApprovedSize(), senderListSizes.getWts(),
				senderSizesSignature, receiverKeyBS);
	}

	private void verifyReceiveAmount(long timestamp, ByteString senderKeyBS, ByteString receiverKeyBS, int amount)
			throws SQLException, TransferNotFoundException {
		if (!db.checkPendingTransferExists(timestamp, senderKeyBS, receiverKeyBS, amount)) throw new TransferNotFoundException();
	}

	public TransfersRecord getApprovedTransfers(ByteString publicKey) throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKey)) throw new AccountDoesNotExistException();
		return db.getApprovedTransfersOfAccount(publicKey);
	}
}
