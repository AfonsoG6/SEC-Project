package pt.tecnico.sec.bftb.server;

import java.security.PublicKey;

public class Transfer {

    private PublicKey sourceKey;
    private PublicKey destinationKey;
    private int amount;
    private boolean pending;
    // approved transfers could also be a subclass instead of an attribute that distinguishes them
    // TODO: needs a signature too for non repudiation

    public Transfer(PublicKey sourceKey, PublicKey destinationKey, int amount) {
        this.sourceKey = sourceKey;
        this.destinationKey = destinationKey;
        this.amount = amount;
        this.pending = true;
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

    public void approve() {
        this.pending = false;
    }

    @Override
    public String toString() {
        return "Amount: " + this.amount; // TODO: what else can we put here?
    }
    
}
