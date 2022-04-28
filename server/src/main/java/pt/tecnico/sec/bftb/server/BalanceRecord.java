package pt.tecnico.sec.bftb.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.bftb.grpc.Server.Balance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class BalanceRecord {
	private final Balance balance;
	private final ByteString signature;

	public BalanceRecord(Balance balance, ByteString signature) {
		this.balance = balance;
		this.signature = signature;
	}

	public BalanceRecord(ResultSet rs) throws SQLException {
		Balance.Builder builder = Balance.newBuilder();
		builder.setValue(rs.getInt("balance"));
		builder.setWts(rs.getInt("balance_wts"));
		this.balance = builder.build();
		this.signature = ByteString.copyFrom(Base64.getDecoder().decode(rs.getString("balance_signature")));
	}

	public Balance getBalance() {
		return balance;
	}

	public ByteString getSignature() {
		return signature;
	}
}
