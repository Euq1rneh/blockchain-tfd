package datastructures;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class Block implements Serializable{

	private byte[] hash;
	private final int epoch;
	private final int length;
	private final Transaction[] transactions;
	
	public Block(int epoch, int length, Transaction[] transactions, Block prevBlock) {
		this.epoch = epoch;
		this.length = length;
		this.transactions = transactions;
		calculateHash(prevBlock);
	}
	
	public byte[] getPreviousBlockHash() {
		return hash;
	}
	
	public int getEpoch() {
		return epoch;
	}
	
	public int getLength() {
		return length;
	}
	
	public Transaction[] getTransactions() {
		return transactions;
	}
	
	private void calculateHash(Block prevBlock) {
		if(prevBlock == null) {
			//genesis block only has length 1 for the hash
			hash = new byte[] {0x00};
			return;
		}
		//TODO implement SHA1 hash
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(hash);
		result = prime * result + Arrays.hashCode(transactions);
		result = prime * result + Objects.hash(epoch, length);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Block other = (Block) obj;
		return epoch == other.epoch && length == other.length
				&& Arrays.equals(transactions, other.transactions);// && Arrays.equals(hash, other.hash);
	}
	
	
}
