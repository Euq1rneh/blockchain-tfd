package datastructures;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class Block implements Serializable{

	private static final long serialVersionUID = -4312015834577135441L;
	
	private byte[] hash;
	private final int epoch;
	private final int length;
	private final Transaction[] transactions;
	private final Block[] parentChain;
	private boolean notarized;
	
	public Block(int epoch, int length, Transaction[] transactions, Block[] parentChain, Block prevBlock) {
		this.epoch = epoch;
		this.length = length;
		this.transactions = transactions;
		this.parentChain = parentChain;
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
	
	public boolean isNotarized() {
		return notarized;
	}
	
	public void notarize() {
		notarized = true;
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
