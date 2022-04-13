package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.grpc.Server.Transfer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class TransferUtils {

	public static List<Transfer> fromResultSet(ResultSet rs) throws SQLException {
		List<Transfer> transfers = new ArrayList<>();
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
		}
		return transfers;
	}

	public static String toString(Transfer transfer) {
		return "$" + transfer.getAmount() + " from "
				+ Base64.getEncoder().encodeToString(transfer.getSenderKey().toByteArray())
				+ " to "
				+ Base64.getEncoder().encodeToString(transfer.getReceiverKey().toByteArray());
	}
}
