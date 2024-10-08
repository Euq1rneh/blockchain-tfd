package datastructures;

public class Transaction {
	
	private final int sender;
	private final int receiver;
	private final double amount;
	private int id;
	
	public Transaction(int sender, int receiver, double amount) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
		
		setTransactionId();
	}
	
	public int getSender() {
		return sender;
	}
	
	public int getReceiver() {
		return receiver;
	}
	
	public double getAmount() {
		return amount;
	}
	
	public int getId() {
		return id;
	}
	
	private void setTransactionId() {
		//set transaction id ?????
	}
}
