package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.server.exceptions.*;
import pt.tecnico.sec.bftb.grpc.Server.Transfer;

import java.sql.SQLException;
import java.util.List;

public class Server {
	private static final long TIMESTAMP_TOLERANCE = 5000;
	private static final int INITIAL_BALANCE = 100;

	SQLiteDatabase db;
	int replicaID;

	public Server(int replicaID) throws ServerInitializationFailedException {
		try {
			Resources.init();
			this.replicaID = replicaID;
			this.db = new SQLiteDatabase(replicaID);
		}
		catch (DirectoryCreationFailedException | SQLException e) {
			throw new ServerInitializationFailedException(e);
		}
	}

	public void openAccount(ByteString publicKey)
			throws AccountAlreadyExistsException, SQLException {
		if (db.checkAccountExists(publicKey)) throw new AccountAlreadyExistsException();

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
		ByteString sourceKey = transfer.getSenderKey();
		ByteString destinationKey = transfer.getReceiverKey();

		verifySendAmount(timestamp, amount, sourceKey, destinationKey);

		// TRANSACTION
		db.writeAccountBalance(sourceKey, db.readAccountBalance(sourceKey) - transfer.getAmount());
		db.insertTransfer(timestamp, sourceKey, destinationKey, amount, senderSignature);
	}

	private void verifySendAmount(long timestamp, int amount, ByteString sourceKey, ByteString destinationKey)
			throws AmountTooLowException, InvalidTimestampException, SQLException, AccountDoesNotExistException,
			BalanceTooLowException {
		if (amount <= 0) throw new AmountTooLowException();
		long currentTime = System.currentTimeMillis();
		if (timestamp > currentTime || timestamp < currentTime - TIMESTAMP_TOLERANCE) throw new InvalidTimestampException();
		if (!db.checkAccountExists(sourceKey)) throw new AccountDoesNotExistException();
		if (!db.checkAccountExists(destinationKey)) throw new AccountDoesNotExistException();
		if (amount >= db.readAccountBalance(sourceKey)) throw new BalanceTooLowException();
	}

	public void receiveAmount(Transfer transfer, ByteString receiverSignature)
			throws TransferNotFoundException, SQLException {
		long timestamp = transfer.getTimestamp();
		ByteString sourceKey = transfer.getSenderKey();
		ByteString destinationKey = transfer.getReceiverKey();
		int amount = transfer.getAmount();

		verifyReceiveAmount(timestamp, sourceKey, destinationKey, amount);

		// TRANSACTION
		db.writeAccountBalance(destinationKey, db.readAccountBalance(destinationKey) + transfer.getAmount());
		db.updateTransferToApproved(timestamp, sourceKey, destinationKey, receiverSignature);
	}

	private void verifyReceiveAmount(long timestamp, ByteString sourceKey, ByteString destinationKey, int amount)
			throws SQLException, TransferNotFoundException {
		if (!db.checkPendingTransferExists(timestamp, sourceKey, destinationKey, amount)) throw new TransferNotFoundException();
	}

	public List<Transfer> getApprovedTransfers(ByteString publicKey) throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKey)) throw new AccountDoesNotExistException();
		return db.getAllTransfersOfAccount(publicKey);
	}
}
