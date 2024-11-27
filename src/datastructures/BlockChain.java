package datastructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockChain {

	private final HashDag<Integer> dag; // The DAG representing the blockchain
	private final Set<Integer> heads; // The current heads (latest blocks of the blockchain)

	public BlockChain() {
		this.dag = new HashDag<Integer>();
		this.heads = new HashSet<Integer>();
	}
	
	public void addGenesisBlock() {
		dag.add(0);
	}
	
	/**
	 * Adds a block to the blockchain
	 * @param parentBlock the parent of the block to add
	 * @param newBlock the new block to add
	 */
	public void addBlock(Integer parentBlockEpoch, Integer newBlockEpoch) {
		// Add the new block to the DAG, with the parent block pointing to it
		dag.put(parentBlockEpoch, newBlockEpoch);

		// The new block is now a head of the blockchain
		heads.add(newBlockEpoch);
	}

	/**
	 * Returns the longest chain found. In case of a tie returns the first one it found
	 * @return the biggest chain it found
	 */
	public List<Integer> getLongestChain() {
		List<Integer> longestChain = new ArrayList<>();
		int maxLength = 0;

		for (Integer head : heads) {
			List<Integer> chain = getChainFromHead(head);
			if (chain.size() > maxLength) {
				maxLength = chain.size();
				longestChain = chain;
			}
		}

		return longestChain;
	}
	
	public boolean resolveBlockchain(List<Integer> finalizedchain) {
		return dag.retainAll(finalizedchain);
	}

	/**
	 * Returns the subchain from each head
	 * @param head the head to start the count from 
	 * @return the subchain from the head
	 */
	private List<Integer> getChainFromHead(Integer head) {
		List<Integer> chain = new ArrayList<>();
		Integer currentBlock = head;

		// Traverse backwards to find the whole chain
		while (currentBlock != null) {
			chain.add(currentBlock);
			Set<Integer> parents = dag.getIncoming(currentBlock);

			// Reached the root (genesis block)
			if (parents.isEmpty()) {
				break; 
			}

			// Choose one parent (assuming we have a single parent path)
			currentBlock = parents.iterator().next();
		}

		// Reverse the chain to start from the genesis block
		Collections.reverse(chain);
		return chain;
	}

	@Override
	public String toString() {
		return dag.toString();
	}
	
	// Example usage:
//	public static void main(String[] args) {
//
//		// Add blocks and forks to the blockchain
//		addBlock(1, 2); // Block 1 -> Block 2
//		addBlock(1, 3); // Block 1 -> Block 3 (fork)
//		addBlock(2, 4); // Block 2 -> Block 4
//		addBlock(4, 5); // Block 4 -> Block 5
//		addBlock(2, 6); // Block 2 -> Block 6
//		addBlock(5, 8); // Block 5 -> Block 8
//		addBlock(6, 7); // Block 6 -> Block 7
//		addBlock(8, 9); // Block 8 -> Block 9 (fork continuation)
//
//		// Resolve the longest chain using the longest chain rule
//		List<Integer> longestChain = resolveLongestChain();
//		System.out.println("Longest Chain: " + longestChain);
//	}

}
