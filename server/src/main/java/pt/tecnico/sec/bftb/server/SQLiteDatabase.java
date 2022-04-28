package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.grpc.Server.Transfer;
import pt.tecnico.sec.bftb.server.exceptions.TransferNotFoundException;

import java.net.URISyntaxException;
import java.sql.*;
import java.util.Base64;
import java.util.List;

public class SQLiteDatabase {
	private final int replicaID;

	public SQLiteDatabase(int replicaID) throws SQLException {
		this.replicaID = replicaID;
		initializeDatabase();
	}

	private void initializeDatabase() throws SQLException {
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				String sql1 = "CREATE TABLE IF NOT EXISTS accounts(" +
						"pubkey TEXT PRIMARY KEY, " +
						"balance INTEGER NOT NULL, " +
						"balance_wts INTEGER NOT NULL," +
						"balance_signature TEXT NOT NULL," +
						"pending_size INTEGER NOT NULL," +
						"approved_size INTEGER NOT NULL," +
						"sizes_wts INTEGER NOT NULL," +
						"sizes_signature TEXT NOT NULL," +
						"sizes_signer_pubkey TEXT NOT NULL)";
				String sql2 = "CREATE TABLE IF NOT EXISTS transfers(" +
						"timestamp INTEGER NOT NULL, " +
						"sender_pubkey TEXT NOT NULL, " +
						"receiver_pubkey TEXT NOT NULL, " +
						"amount INTEGER NOT NULL, " +
						"sender_signature TEXT NOT NULL, " +
						"receiver_signature TEXT, " +
						"FOREIGN KEY (sender_pubkey) REFERENCES accounts(pubkey), " +
						"FOREIGN KEY (receiver_pubkey) REFERENCES accounts(pubkey), " +
						"CONSTRAINT pk PRIMARY KEY(timestamp, sender_pubkey, receiver_pubkey))";
				stmt.execute(sql1);
				stmt.execute(sql2);
			}
		}
	}

	private Connection getConnection() throws SQLException {
		try {
			String url = "jdbc:sqlite:" + Resources.getAbsoluteDatabasePath(replicaID);
			return DriverManager.getConnection(url);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean checkAccountExists(ByteString publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM accounts WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					return rs.next();
				}
			}
		}
	}

	public void insertAccount(ByteString publicKey, int balance, int balanceWts, ByteString balanceSignature,
			int pendingSize, int approvedSize, int sizesWts, ByteString sizesSignature) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "INSERT INTO accounts(pubkey, balance, balance_wts, balance_signature, pending_size, approved_size, sizes_wts, sizes_signature, sizes_signer_pubkey) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				stmt.setInt(2, balance);
				stmt.setInt(3, balanceWts);
				stmt.setString(4, Base64.getEncoder().encodeToString(balanceSignature.toByteArray()));
				stmt.setInt(5, pendingSize);
				stmt.setInt(6, approvedSize);
				stmt.setInt(7, sizesWts);
				stmt.setString(8, Base64.getEncoder().encodeToString(sizesSignature.toByteArray()));
				stmt.setString(9, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				stmt.executeUpdate();
			}
		}
	}

	public int readAccountBalance(ByteString publicKeyBS) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT balance FROM accounts " +
					"WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKeyBS.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("balance");
					}
					else {
						throw new SQLException();
					}
				}
			}
		}
	}

	public BalanceRecord readAccountBalanceRecord(ByteString publicKeyBS) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM accounts " +
					"WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKeyBS.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return new BalanceRecord(rs);
					}
					else {
						throw new SQLException();
					}
				}
			}
		}
	}

	public ListSizesRecord readAccountListSizesRecord(ByteString publicKeyBS) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM accounts " +
					"WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKeyBS.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return new ListSizesRecord(rs);
					}
					else {
						throw new SQLException();
					}
				}
			}
		}
	}

	public void updateAccountBalance(ByteString accountPublicKey, int balance, int wts, ByteString signature)
			throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "UPDATE accounts SET " +
					"balance = ? ," +
					"balance_wts = ? ," +
					"balance_signature = ? " +
					"WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, balance);
				stmt.setInt(2, wts);
				stmt.setString(3, Base64.getEncoder().encodeToString(signature.toByteArray()));
				stmt.setString(4, Base64.getEncoder().encodeToString(accountPublicKey.toByteArray()));
				stmt.executeUpdate();
			}
		}
	}

	public void updateAccountListSizes(ByteString accountPublicKey, int pendingSize, int approvedSize, int sizesWts,
			ByteString sizesSignature, ByteString sizesSignerPublicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "UPDATE accounts SET " +
					"pending_size = ? ," +
					"approved_size = ? ," +
					"sizes_wts = ? ," +
					"sizes_signature = ? ," +
					"sizes_signer_pubkey = ? " +
					"WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, pendingSize);
				stmt.setInt(2, approvedSize);
				stmt.setInt(3, sizesWts);
				stmt.setString(4, Base64.getEncoder().encodeToString(sizesSignature.toByteArray()));
				stmt.setString(5, Base64.getEncoder().encodeToString(sizesSignerPublicKey.toByteArray()));
				stmt.setString(6, Base64.getEncoder().encodeToString(accountPublicKey.toByteArray()));
				stmt.executeUpdate();
			}
		}
	}

	public void updateAccountAll(ByteString accountPublicKey, int balance, int balanceWts, ByteString balanceSignature,
			int pendingSize, int approvedSize, int sizesWts, ByteString sizesSignature, ByteString sizesSignerPublicKey)
			throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "UPDATE accounts SET " +
					"balance = ? ," +
					"balance_wts = ? ," +
					"balance_signature = ? ," +
					"pending_size = ? ," +
					"approved_size = ? ," +
					"sizes_wts = ? ," +
					"sizes_signature = ? ," +
					"sizes_signer_pubkey = ? " +
					"WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, balance);
				stmt.setInt(2, balanceWts);
				stmt.setString(3, Base64.getEncoder().encodeToString(balanceSignature.toByteArray()));
				stmt.setInt(4, pendingSize);
				stmt.setInt(5, approvedSize);
				stmt.setInt(6, sizesWts);
				stmt.setString(7, Base64.getEncoder().encodeToString(sizesSignature.toByteArray()));
				stmt.setString(8, Base64.getEncoder().encodeToString(sizesSignerPublicKey.toByteArray()));
				stmt.setString(9, Base64.getEncoder().encodeToString(accountPublicKey.toByteArray()));
				stmt.executeUpdate();
			}
		}
	}

	public void insertTransfer(long timestamp, ByteString senderPublicKey, ByteString receiverPublicKey, int amount, ByteString senderSignature)
			throws
			SQLException {
		try (Connection conn = getConnection()) {
			String sql = "INSERT INTO transfers(timestamp, sender_pubkey, receiver_pubkey, amount, sender_signature) VALUES (?, ?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, timestamp);
				stmt.setString(2, Base64.getEncoder().encodeToString(senderPublicKey.toByteArray()));
				stmt.setString(3, Base64.getEncoder().encodeToString(receiverPublicKey.toByteArray()));
				stmt.setInt(4, amount);
				stmt.setString(5, Base64.getEncoder().encodeToString(senderSignature.toByteArray()));
				stmt.executeUpdate();
			}
		}
	}

	public TransfersRecord getIncomingPendingTransfersOfAccount(ByteString publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers " +
					"WHERE receiver_pubkey = ? " +
					"AND receiver_signature IS NULL " +
					"ORDER BY timestamp";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					return new TransfersRecord(rs);
				}
			}
		}
	}

	public TransfersRecord getApprovedTransfersOfAccount(ByteString publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers " +
					"WHERE (sender_pubkey = ? OR receiver_pubkey = ?) " +
					"AND receiver_signature IS NOT NULL " +
					"ORDER BY timestamp";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				stmt.setString(2, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					return new TransfersRecord(rs);
				}
			}
		}
	}

	public Transfer getTransfer(long timestamp, ByteString senderPublicKey, ByteString receiverPublicKey)
			throws SQLException, TransferNotFoundException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers " +
					"WHERE timestamp = ? " +
					"AND sender_pubkey = ? " +
					"AND receiver_pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, timestamp);
				stmt.setString(2, Base64.getEncoder().encodeToString(senderPublicKey.toByteArray()));
				stmt.setString(3, Base64.getEncoder().encodeToString(receiverPublicKey.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					List<Transfer> results = new TransfersRecord(rs).getTransfers();
					if (results.isEmpty()) throw new TransferNotFoundException();
					else return results.get(0);
				}
			}
		}
	}

	public void updateTransferToApproved(long timestamp, ByteString senderPublicKey, ByteString receiverPublicKey, ByteString receiverSignature)
			throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "UPDATE transfers SET receiver_signature = ? " +
					"WHERE timestamp = ? " +
					"AND sender_pubkey = ? " +
					"AND receiver_pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(receiverSignature.toByteArray()));
				stmt.setLong(2, timestamp);
				stmt.setString(3, Base64.getEncoder().encodeToString(senderPublicKey.toByteArray()));
				stmt.setString(4, Base64.getEncoder().encodeToString(receiverPublicKey.toByteArray()));
				stmt.executeUpdate();
			}
		}
	}

	public boolean checkPendingTransferExists(long timestamp, ByteString sourceKey, ByteString destinationKey, int amount)
			throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers " +
					"WHERE timestamp = ? " +
					"AND sender_pubkey = ? " +
					"AND receiver_pubkey = ? " +
					"AND amount = ? " +
					"AND receiver_signature IS NULL";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, timestamp);
				stmt.setString(2, Base64.getEncoder().encodeToString(sourceKey.toByteArray()));
				stmt.setString(3, Base64.getEncoder().encodeToString(destinationKey.toByteArray()));
				stmt.setInt(4, amount);
				try (ResultSet rs = stmt.executeQuery()) {
					return rs.next();
				}
			}
		}
	}
}
