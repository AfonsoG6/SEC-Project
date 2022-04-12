package pt.tecnico.sec.bftb.server;

import java.sql.SQLException;
import pt.tecnico.sec.bftb.server.exceptions.TransferNotFoundException;
import pt.tecnico.sec.bftb.grpc.Server.Transfer;

import java.net.URISyntaxException;
import java.security.PublicKey;
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
				stmt.execute("CREATE TABLE IF NOT EXISTS accounts(pubkey TEXT PRIMARY KEY, balance INTEGER)");
				stmt.execute("CREATE TABLE IF NOT EXISTS transfers(timestamp INTEGER, sender_pubkey TEXT, receiver_pubkey TEXT, amount INTEGER, approved BOOLEAN, FOREIGN KEY (sender_pubkey) REFERENCES accounts(pubkey), FOREIGN KEY (receiver_pubkey) REFERENCES accounts(pubkey), CONSTRAINT pk PRIMARY KEY(timestamp, sender_pubkey, receiver_pubkey))");
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

	public boolean checkAccountExists(PublicKey pubkey) throws SQLException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM accounts WHERE pubkey = ?")) {
				stmt.setString(1, Base64.getEncoder().encodeToString(pubkey.getEncoded()));
				try (ResultSet rs = stmt.executeQuery()) {
					return rs.next();
				}
			}
		}
	}

	public void insertAccount(PublicKey pubkey, int balance) throws SQLException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO accounts(pubkey, balance) VALUES(?, ?)")) {
				stmt.setString(1, Base64.getEncoder().encodeToString(pubkey.getEncoded()));
				stmt.setInt(2, balance);
				stmt.executeUpdate();
			}
		}
	}

	public int readAccountBalance(PublicKey publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT balance FROM account_balance WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
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

	public void writeAccountBalance(PublicKey publicKey, int balance) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "UPDATE account_balance SET balance = ? WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, balance);
				stmt.setString(2, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				stmt.executeUpdate();
			}
		}
	}

	public void insertTransfer(long timestamp, PublicKey senderPublicKey, PublicKey receiverPublicKey, int amount) throws
			SQLException {
		try (Connection conn = getConnection()) {
			String sql = "INSERT INTO transfers(timestamp, sender_pubkey, receiver_pubkey, amount) VALUES (?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, timestamp);
				stmt.setString(2, Base64.getEncoder().encodeToString(senderPublicKey.getEncoded()));
				stmt.setString(3, Base64.getEncoder().encodeToString(receiverPublicKey.getEncoded()));
				stmt.setInt(4, amount);
				stmt.executeUpdate();
			}
		}
	}

	public List<Transfer> getIncomingPendingTransfersOfAccount(PublicKey publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers WHERE receiver_pubkey = ? AND approved = FALSE ORDER BY timestamp ASC";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				try (ResultSet rs = stmt.executeQuery()) {
					return TransferUtils.fromResultSet(rs);
				}
			}
		}
	}

	public List<Transfer> getAllTransfersOfAccount(PublicKey publicKey) throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers WHERE sender_pubkey = ? OR receiver_pubkey = ? ORDER BY timestamp ASC";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				stmt.setString(2, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				try (ResultSet rs = stmt.executeQuery()) {
					return TransferUtils.fromResultSet(rs);
				}
			}
		}
	}

	public Transfer getTransfer(long timestamp, PublicKey senderPublicKey, PublicKey receiverPublicKey)
			throws SQLException, TransferNotFoundException {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers WHERE timestamp = ? AND sender_pubkey = ? AND receiver_pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, timestamp);
				stmt.setString(2, Base64.getEncoder().encodeToString(senderPublicKey.getEncoded()));
				stmt.setString(3, Base64.getEncoder().encodeToString(receiverPublicKey.getEncoded()));
				try (ResultSet rs = stmt.executeQuery()) {
					if (!rs.first()) throw new TransferNotFoundException();
					return TransferUtils.fromResultSet(rs).get(0);
				}
			}
		}
	}

	public void updateTransferToApproved(long timestamp, PublicKey sourceKey, PublicKey destinationKey)
			throws SQLException {
		try (Connection conn = getConnection()) {
			String sql = "UPDATE transfers SET approved = TRUE WHERE timestamp = ? AND sender_pubkey = ? AND receiver_pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, timestamp);
				stmt.setString(2, Base64.getEncoder().encodeToString(sourceKey.getEncoded()));
				stmt.setString(3, Base64.getEncoder().encodeToString(destinationKey.getEncoded()));
				stmt.executeUpdate();
			}
		}
	}
}
