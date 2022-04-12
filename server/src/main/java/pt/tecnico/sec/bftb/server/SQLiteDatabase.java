package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.ConnectionToDatabaseFailed;

import java.net.URISyntaxException;
import java.security.PublicKey;
import java.sql.*;
import java.util.Base64;
import java.util.List;

public class SQLiteDatabase {
	private final int replicaID;

	public SQLiteDatabase(int replicaID) throws ConnectionToDatabaseFailed {
		this.replicaID = replicaID;
		initializeDatabase();
	}

	private void initializeDatabase() throws ConnectionToDatabaseFailed {
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE IF NOT EXISTS accounts(pubkey TEXT PRIMARY KEY, balance INTEGER)");
				stmt.execute("CREATE TABLE IF NOT EXISTS transfers(id INTEGER PRIMARY KEY, sender_pubkey TEXT, receiver_pubkey TEXT, amount INTEGER, approved BOOLEAN, FOREIGN KEY (sender_pubkey) REFERENCES accounts(pubkey), FOREIGN KEY (receiver_pubkey) REFERENCES accounts(pubkey))");
			}
		}
		catch (SQLException e) {
			// TODO: specify the exception
			throw new ConnectionToDatabaseFailed(e);
		}
	}

	private Connection getConnection() throws ConnectionToDatabaseFailed {
		try {
			String url = "jdbc:sqlite:" + Resources.getAbsoluteDatabasePath(replicaID);
			return DriverManager.getConnection(url);
		}
		catch (URISyntaxException | SQLException e) {
			throw new ConnectionToDatabaseFailed(e);
		}
	}

	public int readAccountBalance(PublicKey publicKey) throws ConnectionToDatabaseFailed {
		try (Connection conn = getConnection()) {
			String sql = "SELECT balance FROM account_balance WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("balance");
					}
					else {
						// TODO: specify the exception
						throw new ConnectionToDatabaseFailed();
					}
				}
			}
		}
		catch (SQLException e) {
			// TODO: specify the exception
			throw new ConnectionToDatabaseFailed(e);
		}
	}

	public void writeAccountBalance(PublicKey publicKey, int balance) throws ConnectionToDatabaseFailed {
		try (Connection conn = getConnection()) {
			String sql = "UPDATE account_balance SET balance = ? WHERE pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, balance);
				stmt.setString(2, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				stmt.executeUpdate();
			}
		}
		catch (SQLException e) {
			// TODO: specify the exception
			throw new ConnectionToDatabaseFailed(e);
		}
	}

	public void writeTransfer(int transferID, PublicKey senderPublicKey, PublicKey receiverPublicKey, int amount) throws ConnectionToDatabaseFailed {
		try (Connection conn = getConnection()) {
			String sql = "INSERT INTO transfers(id, sender_pubkey, receiver_pubkey, amount) VALUES (?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, transferID);
				stmt.setString(2, Base64.getEncoder().encodeToString(senderPublicKey.getEncoded()));
				stmt.setString(3, Base64.getEncoder().encodeToString(receiverPublicKey.getEncoded()));
				stmt.setInt(4, amount);
				stmt.executeUpdate();
			}
		}
		catch (SQLException e) {
			// TODO: specify the exception
			throw new ConnectionToDatabaseFailed(e);
		}
	}

	public List<Transfer> readIncomingTransfersOfAccount(PublicKey publicKey) throws ConnectionToDatabaseFailed {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers WHERE receiver_pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				try (ResultSet rs = stmt.executeQuery()) {
					return Transfer.fromResultSet(rs);
				}
			}
		}
		catch (SQLException e) {
			// TODO: specify the exception
			throw new ConnectionToDatabaseFailed(e);
		}
	}

	public List<Transfer> readAllTransfersOfAccount(PublicKey publicKey) throws ConnectionToDatabaseFailed {
		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM transfers WHERE sender_pubkey = ? OR receiver_pubkey = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				stmt.setString(2, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
				try (ResultSet rs = stmt.executeQuery()) {
					return Transfer.fromResultSet(rs);
				}
			}
		}
		catch (SQLException e) {
			// TODO: specify the exception
			throw new ConnectionToDatabaseFailed(e);
		}
	}

}
