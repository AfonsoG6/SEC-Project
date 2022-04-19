package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.grpc.Server.ListSizes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class ListSizesRecord {
	private final ListSizes listSizes;
	private final ByteString signature;
	private final ByteString signerPublicKeyBS;

	public ListSizesRecord(ListSizes receiverListSizes, ByteString signature, ByteString signerPublicKey) {
		this.listSizes = receiverListSizes;
		this.signature = signature;
		this.signerPublicKeyBS = signerPublicKey;
	}

	public ListSizesRecord(ResultSet rs) throws SQLException {
		ListSizes.Builder builder = ListSizes.newBuilder();
		builder.setPendingSize(rs.getInt("pending_size"));
		builder.setApprovedSize(rs.getInt("approved_size"));
		builder.setWts(rs.getInt("sizes_wts"));
		this.listSizes = builder.build();
		this.signature = ByteString.copyFrom(Base64.getDecoder().decode(rs.getString("sizes_signature")));
		this.signerPublicKeyBS = ByteString.copyFrom(Base64.getDecoder().decode(rs.getString("sizes_signer_pubkey")));
	}

	public ListSizes getListSizes() {
		return listSizes;
	}

	public ByteString getSignature() {
		return signature;
	}

	public ByteString getSignerPublicKeyBS() {
		return signerPublicKeyBS;
	}
}
