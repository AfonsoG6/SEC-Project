package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.grpc.Server.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class TransfersRecord {
	private List<Transfer> transfers;
	private List<ByteString> senderSignatures;
	private List<ByteString> receiverSignatures;

	public TransfersRecord(List<Transfer> transfers, List<ByteString> signatures) {
		this.transfers = transfers;
		this.senderSignatures = signatures;
	}

	public TransfersRecord(ResultSet rs) throws SQLException {
		this.transfers = new ArrayList<>();
		this.senderSignatures = new ArrayList<>();
		this.receiverSignatures = new ArrayList<>();
		while (rs.next()) {
			long timestamp = rs.getLong("timestamp");
			byte[] sourceKeyBytes = Base64.getDecoder().decode(rs.getString("sender_pubkey"));
			byte[] destinationKeyBytes = Base64.getDecoder().decode(rs.getString("receiver_pubkey"));
			int amount = rs.getInt("amount");

			Transfer.Builder builder = Transfer.newBuilder();
			builder.setTimestamp(timestamp);
			builder.setSenderKey(ByteString.copyFrom(sourceKeyBytes));
			builder.setReceiverKey(ByteString.copyFrom(destinationKeyBytes));
			builder.setAmount(amount);
			transfers.add(builder.build());
			ByteString senderSignature = ByteString.copyFrom(Base64.getDecoder().decode(rs.getString("sender_signature")));
			senderSignatures.add(senderSignature);
			ByteString receiverSignature = ByteString.copyFrom(Base64.getDecoder().decode(rs.getString("receiver_signature")));
			receiverSignatures.add(receiverSignature);
		}
	}

	public List<Transfer> getTransfers() {
		return transfers;
	}

	public List<ByteString> getSenderSignatures() {
		return senderSignatures;
	}

	public List<ByteString> getReceiverSignatures() {
		return receiverSignatures;
	}

}
