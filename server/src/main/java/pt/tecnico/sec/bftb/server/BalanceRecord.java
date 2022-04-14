package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.grpc.Server.*;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BalanceRecord {
	private final Balance balance;
	private final byte[] signature;

	public BalanceRecord(Balance balance, byte[] signature) {
		this.balance = balance;
		this.signature = signature;
	}

	public BalanceRecord(ResultSet rs) throws SQLException {
		Balance.Builder builder = Balance.newBuilder();
		builder.setValue(rs.getInt("balance"));
		builder.setWts(rs.getInt("wts"));
		this.balance = builder.build();
		this.signature = rs.getBytes("sign");
	}

	public Balance getBalance() {
		return balance;
	}

	public byte[] getSignature() {
		return signature;
	}
}
