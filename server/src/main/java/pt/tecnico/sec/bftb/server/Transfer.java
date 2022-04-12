package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.ConnectionToDatabaseFailed;

import java.io.Serial;
import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Transfer implements Serializable {
	@Serial
	private static final long serialVersionUID = 202203261535L;

	private final long id;
	private final PublicKey sourceKey;
	private final PublicKey destinationKey;
	private final int amount;
	private boolean pending;

	public Transfer(long id, PublicKey sourceKey, PublicKey destinationKey, int amount) {
		this.id = id;
		this.sourceKey = sourceKey;
		this.destinationKey = destinationKey;
		this.amount = amount;
		this.pending = true;
	}

	public static List<Transfer> fromResultSet(ResultSet rs) throws ConnectionToDatabaseFailed {
		try {
			List<Transfer> transfers = new ArrayList<>();
			while (rs.next()) {
				long id = rs.getLong("id");
				byte[] sourceKeyBytes = Base64.getDecoder().decode(rs.getString("source_key"));
				PublicKey sourceKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(sourceKeyBytes));
				byte[] destinationKeyBytes = Base64.getDecoder().decode(rs.getString("destination_key"));
				PublicKey destinationKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(destinationKeyBytes));
				int amount = rs.getInt("amount");
				transfers.add(new Transfer(id, sourceKey, destinationKey, amount));
			}
		}
		catch (SQLException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			// TODO: handle exception correctly
			throw new ConnectionToDatabaseFailed(e);
		}
	}

	public long getID() {
		return this.id;
	}

	public PublicKey getSourceKey() {
		return sourceKey;
	}

	public PublicKey getDestinationKey() {
		return destinationKey;
	}

	public int getAmount() {
		return amount;
	}

	public boolean getPending() {
		return pending;
	}

	public void approve() {
		this.pending = false;
	}

	@Override
	public String toString() {
		return ((this.pending) ? "[Pending]" : "[Approved]")
				+ " $" + this.amount + " from "
				+ Base64.getEncoder().encodeToString(this.sourceKey.getEncoded())
				+ " to "
				+ Base64.getEncoder().encodeToString(this.destinationKey.getEncoded());
	}
}
