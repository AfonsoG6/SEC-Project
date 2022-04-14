package pt.tecnico.sec.bftb.server;

import java.sql.SQLException;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.server.exceptions.TransferNotFoundException;
import pt.tecnico.sec.bftb.grpc.Server.Transfer;

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
				stmt.execute("CREATE TABLE IF NOT EXISTS accounts(" +
									"pubkey TEXT PRIMARY KEY, " +
									"balance INTEGER NOT NULL, " +
									"wts INTEGER NOT NULL," +
									"sign TEXT NOT NULL)");
				stmt.execute("CREATE TABLE IF NOT EXISTS transfers(" +
									"timestamp INTEGER NOT NULL, " +
									"sender_pubkey TEXT NOT NULL, " +
									"receiver_pubkey TEXT NOT NULL, " +
									"amount INTEGER NOT NULL, " +
									"sender_sign TEXT NOT NULL, " +
									"receiver_sign TEXT, " +
									"FOREIGN KEY (sender_pubkey) REFERENCES accounts(pubkey), " +
									"FOREIGN KEY (receiver_pubkey) REFERENCES accounts(pubkey), " +
									"CONSTRAINT pk PRIMARY KEY(timestamp, sender_pubkey, receiver_pubkey))");
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

	public void insertAccount(ByteString publicKey, int balance) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "INSERT INTO accounts(pubkey, balance) VALUES(?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				stmt.setInt(2, balance);
				stmt.executeUpdate();
			}
		}
	}

	public int readAccountBalance(ByteString publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql =    "SELECT balance FROM accounts " +
							"WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
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

	public void writeAccountBalance(ByteString publicKey, int balance) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql =    "UPDATE accounts SET balance = ? " +
							"WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, balance);
				stmt.setString(2, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				stmt.executeUpdate();
			}
		}
	}

	public void insertTransfer(long timestamp, ByteString senderPublicKey, ByteString receiverPublicKey, int amount, ByteString senderSignature) throws
			SQLException {
		try (Connection conn = getConnection()) {
			String sql = "INSERT INTO transfers(timestamp, sender_pubkey, receiver_pubkey, amount, sender_sign) VALUES (?, ?, ?, ?, ?)";
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

	public List<Transfer> getIncomingPendingTransfersOfAccount(ByteString publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql =    "SELECT * FROM transfers " +
							"WHERE receiver_pubkey = ? " +
							"AND receiver_sign IS NULL " +
							"ORDER BY timestamp";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					return TransferUtils.fromResultSet(rs);
				}
			}
		}
	}

	public List<Transfer> getAllTransfersOfAccount(ByteString publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql =    "SELECT * FROM transfers " +
							"WHERE (sender_pubkey = ? OR receiver_pubkey = ?) " +
							"AND receiver_sign IS NOT NULL " +
							"ORDER BY timestamp";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				stmt.setString(2, Base64.getEncoder().encodeToString(publicKey.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					return TransferUtils.fromResultSet(rs);
				}
			}
		}
	}

	public Transfer getTransfer(long timestamp, ByteString senderPublicKey, ByteString receiverPublicKey)
			throws SQLException, TransferNotFoundException {
		try (Connection conn = getConnection()) {
			String sql =    "SELECT * FROM transfers " +
							"WHERE timestamp = ? " +
							"AND sender_pubkey = ? " +
							"AND receiver_pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, timestamp);
				stmt.setString(2, Base64.getEncoder().encodeToString(senderPublicKey.toByteArray()));
				stmt.setString(3, Base64.getEncoder().encodeToString(receiverPublicKey.toByteArray()));
				try (ResultSet rs = stmt.executeQuery()) {
					List<Transfer> results = TransferUtils.fromResultSet(rs);
					if (results.isEmpty()) throw new TransferNotFoundException();
					else return results.get(0);
				}
			}
		}
	}

	public void updateTransferToApproved(long timestamp, ByteString senderPublicKey, ByteString receiverPublicKey, ByteString receiverSignature)
			throws SQLException {
		try (Connection conn = getConnection()) {
			String sql =    "UPDATE transfers SET receiver_sign = ? " +
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
					"AND receiver_sign IS NULL";
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
