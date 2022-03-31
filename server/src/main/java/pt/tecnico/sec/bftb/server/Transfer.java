package pt.tecnico.sec.bftb.server;

import java.io.Serial;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.Base64;

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
