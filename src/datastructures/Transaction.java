package datastructures;

import java.io.Serializable;
import java.util.Objects;

public class Transaction implements Serializable{
	
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

	@Override
	public int hashCode() {
		return Objects.hash(amount, id, receiver, sender);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transaction other = (Transaction) obj;
		return Double.doubleToLongBits(amount) == Double.doubleToLongBits(other.amount) && id == other.id
				&& receiver == other.receiver && sender == other.sender;
	}
	
	
}
