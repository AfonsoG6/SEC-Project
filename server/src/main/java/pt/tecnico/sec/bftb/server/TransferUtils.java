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
			byte[] sourceKeyBytes = Base64.getDecoder().decode(rs.getString("source_key"));
			byte[] destinationKeyBytes = Base64.getDecoder().decode(rs.getString("destination_key"));
			int amount = rs.getInt("amount");
			boolean approved = rs.getBoolean("approved");

			Transfer.Builder builder = Transfer.newBuilder();
			builder.setTimestamp(timestamp);
			builder.setSourceKey(ByteString.copyFrom(sourceKeyBytes));
			builder.setDestinationKey(ByteString.copyFrom(destinationKeyBytes));
			builder.setAmount(amount);
			builder.setApproved(approved);
			transfers.add(builder.build());
		}
		return transfers;
	}

	public static String toString(Transfer transfer) {
		return ((!transfer.getApproved()) ? "[Pending]" : "[Approved]")
				+ " $" + transfer.getAmount() + " from "
				+ Base64.getEncoder().encodeToString(transfer.getSourceKey().toByteArray())
				+ " to "
				+ Base64.getEncoder().encodeToString(transfer.getDestinationKey().toByteArray());
	}
}
