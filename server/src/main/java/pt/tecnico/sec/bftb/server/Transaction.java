package pt.tecnico.sec.bftb.server;

import java.security.PublicKey;

public class Transaction {
    
    private PublicKey sourceKey;
    private PublicKey destinationKey;
    private int amount;
    // TODO: additional field to distinguish pending transactions from processed transactions?
    // TODO: signature too maybe for non repudiation

    public Transaction(PublicKey sourceKey, PublicKey destinationKey, int amount) {
		this.sourceKey = sourceKey;
        this.destinationKey = destinationKey;
        this.amount = amount;
	}
}
