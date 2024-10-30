package datastructures;

public class Block {

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
}
