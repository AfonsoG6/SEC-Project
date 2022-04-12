package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.*;
import pt.tecnico.sec.bftb.grpc.Server.Transfer;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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

	public void openAccount(PublicKey publicKey)
			throws AccountAlreadyExistsException, SQLException {
		if (db.checkAccountExists(publicKey)) throw new AccountAlreadyExistsException();

		db.insertAccount(publicKey, INITIAL_BALANCE);
	}

	// Check Account (part 1):

	public int getAccountBalance(PublicKey publicKey) throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKey)) throw new AccountDoesNotExistException();
		return db.readAccountBalance(publicKey);
	}

	// Check Account (part 2):

	public List<Transfer> getPendingIncomingTransfers(PublicKey publicKey)
			throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKey)) throw new AccountDoesNotExistException();
		return db.getIncomingPendingTransfersOfAccount(publicKey);
	}

	// Send Amount:

	public void sendAmount(long timestamp, PublicKey sourceKey, PublicKey destinationKey, int amount)
			throws AccountDoesNotExistException, AmountTooLowException, BalanceTooLowException, NoSuchAlgorithmException, SQLException, InvalidTimestampException {
		if (amount <= 0) throw new AmountTooLowException();
		long currentTime = System.currentTimeMillis();
		if (timestamp > currentTime || timestamp < currentTime - TIMESTAMP_TOLERANCE) throw new InvalidTimestampException();
		if (db.checkAccountExists(sourceKey)) throw new AccountDoesNotExistException();
		if (db.checkAccountExists(destinationKey)) throw new AccountDoesNotExistException();
		if (amount >= db.readAccountBalance(sourceKey)) throw new BalanceTooLowException();

		db.insertTransfer(timestamp, sourceKey, destinationKey, amount);
	}

	// Receive Amount:

	public void receiveAmount(long timestamp, PublicKey sourceKey, PublicKey destinationKey)
			throws AccountDoesNotExistException, BalanceTooLowException, TransferNotFoundException, NoSuchAlgorithmException, SQLException {
		if (db.checkAccountExists(sourceKey)) throw new AccountDoesNotExistException();
		if (db.checkAccountExists(destinationKey)) throw new AccountDoesNotExistException();

		Transfer transfer = db.getTransfer(timestamp, sourceKey, destinationKey);

		// TRANSACTION
		db.writeAccountBalance(sourceKey, db.readAccountBalance(sourceKey) - transfer.getAmount());
		db.writeAccountBalance(destinationKey, db.readAccountBalance(destinationKey) + transfer.getAmount());
		db.updateTransferToApproved(timestamp, sourceKey, destinationKey);
	}

	// Audit:

	public List<Transfer> getApprovedTransfers(PublicKey publicKey) throws AccountDoesNotExistException, SQLException {
		if (!db.checkAccountExists(publicKey)) throw new AccountDoesNotExistException();
		return db.getAllTransfersOfAccount(publicKey);
	}
}
