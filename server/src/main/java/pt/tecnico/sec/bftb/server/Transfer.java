package pt.tecnico.sec.bftb.server;

import java.security.PublicKey;
import java.util.Base64;

public class Transfer {

    private PublicKey sourceKey;
    private PublicKey destinationKey;
    private int amount;
    private boolean pending;

    private long sourceNonce;
    private byte[] sourceSignature;
    private long destinationNonce;
    private byte[] destinationSignature;

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
        return ((this.pending) ? "[Pending] " : "[Approved] ")
                + Base64.getEncoder().encodeToString(this.sourceKey.getEncoded())
                + "--$" + this.amount + "-->"
                + Base64.getEncoder().encodeToString(this.destinationKey.getEncoded());
    }
    
}
