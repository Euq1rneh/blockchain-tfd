package datastructures;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

public class Transaction implements Serializable{
	
	private static final long serialVersionUID = -4457816105021087700L;
	
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
		Random rd = new Random();
		int nonce = rd.nextInt();
		
		id = nonce * sender;;
	}

	public byte[] getBytes() {
	    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
	         ObjectOutputStream oos = new ObjectOutputStream(bos)) {
	        
	        oos.writeObject(this);
	        oos.flush();
	        
	        return bos.toByteArray();
	        
	    } catch (IOException e) {
	        throw new RuntimeException("Error converting Block to bytes", e);
	    }
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(receiver, sender);
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
