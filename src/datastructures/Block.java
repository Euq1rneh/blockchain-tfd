package datastructures;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Block implements Serializable{

	private static final long serialVersionUID = -4312015834577135441L;
	
	private byte[] hash;
	private final int epoch;
	private final int length;
	private final Transaction[] transactions;
	private final List<Block> parentChain; // to avoid sending another message
	private boolean notarized;
	
	public Block(int epoch, int length, Transaction[] transactions, List<Block> parentChain, Block prevBlock) {
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
	    
	    try {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        md.update(getBytes());
	        hash = md.digest();
	    } catch (NoSuchAlgorithmException e) {
	        throw new RuntimeException("SHA-1 algorithm not available", e);
	    }
		
	}
	
	public boolean isNotarized() {
		return notarized;
	}
	
	public void notarize() {
		notarized = true;
	}

	public List<Block> getParentChain(){
		return parentChain;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(hash);
		result = prime * result + Arrays.hashCode(transactions);
		result = prime * result + Objects.hash(epoch, length, notarized, parentChain);
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
		return epoch == other.epoch && Arrays.equals(hash, other.hash) && length == other.length
				&& notarized == other.notarized && Objects.equals(parentChain, other.parentChain)
				&& Arrays.equals(transactions, other.transactions);
	}
	
	
	
	
}
