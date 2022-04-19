package pt.tecnico.sec.bftb.client;

import pt.tecnico.sec.bftb.grpc.Server.*;

public class InfoForWrite {
	private Balance balance;
	private ListSizes senderListSizes;
	private ListSizes receiverListSizes;

	public InfoForWrite(Balance balance, ListSizes senderListSizes, ListSizes receiverListSizes) {
		this.balance = balance;
		this.senderListSizes = senderListSizes;
		this.receiverListSizes = receiverListSizes;
	}

	public Balance getBalance() {
		return balance;
	}

	public ListSizes getSenderListSizes() {
		return senderListSizes;
	}

	public ListSizes getReceiverListSizes() {
		return receiverListSizes;
	}
}
