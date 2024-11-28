package datastructures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockChain implements Serializable {

	private static final long serialVersionUID = -5986244309823453661L;

	private HashDag<Integer> dag; // The DAG representing the blockchain
	private final HashSet<Integer> heads; // The current heads (latest blocks of the blockchain)

	public BlockChain() {
		this.dag = new HashDag<Integer>();
		this.heads = new HashSet<Integer>();
	}

	public void addGenesisBlock() {
		dag.add(0);
	}

	/**
	 * Adds a block to the blockchain
	 * 
	 * @param parentBlock the parent of the block to add
	 * @param newBlock    the new block to add
	 */
	public void addBlock(Integer parentBlockEpoch, Integer newBlockEpoch) {
		// Add the new block to the DAG, with the parent block pointing to it
		dag.put(parentBlockEpoch, newBlockEpoch);

		// The new block is now a head of the blockchain
		heads.add(newBlockEpoch);
	}

	public void union(BlockChain bc) {
		dag = dag.union(bc.getDAG());
	}

	/**
	 * Returns the longest chain found. In case of a tie returns the first one it
	 * found
	 * 
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

	public boolean contains(int epoch) {
		return dag.contains(epoch);
	}

	public boolean resolveBlockchain(List<Integer> finalizedchain) {
		return dag.retainAll(finalizedchain);
	}

	/**
	 * Returns the subchain from each head
	 * 
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

	/**
	 * Finds the longest path in the given DAG of integers.
	 *
	 * @param dag the DAG of integers
	 * @return the longest path as a list of integers
	 */
	public List<Integer> findLongestPath() {
		// Perform topological sort
		List<Integer> sortedNodes = dag.sort();
		if (sortedNodes == null) {
			throw new IllegalArgumentException("The graph contains a cycle, which is not allowed in a DAG.");
		}

		Map<Integer, Integer> distance = new HashMap<>(); // Stores the longest distance to each node
		Map<Integer, Integer> predecessor = new HashMap<>(); // Stores the predecessor for path reconstruction
		for (Integer node : dag.getNodes()) {
			distance.put(node, Integer.MIN_VALUE); // Initialize distances to negative infinity
		}

		// The root nodes start with a distance of 0
		for (Integer root : dag.getRoots()) {
			distance.put(root, 0);
		}

		// Process each node in topological order
		for (Integer node : sortedNodes) {
			for (Integer neighbor : dag.getOutgoing(node)) {
				int newDistance = distance.get(node) + 1; // Edge weight is assumed to be 1
				if (newDistance > distance.get(neighbor)) {
					distance.put(neighbor, newDistance);
					predecessor.put(neighbor, node);
				}
			}
		}

		// Find the node with the maximum distance
		Integer farthestNode = null;
		int maxDistance = Integer.MIN_VALUE;
		for (Map.Entry<Integer, Integer> entry : distance.entrySet()) {
			if (entry.getValue() > maxDistance) {
				maxDistance = entry.getValue();
				farthestNode = entry.getKey();
			}
		}

		// Reconstruct the longest path
		List<Integer> longestPath = new ArrayList<>();
		while (farthestNode != null) {
			longestPath.add(0, farthestNode);
			farthestNode = predecessor.get(farthestNode);
		}

		return longestPath;
	}

	public HashDag<Integer> getDAG() {
		return dag;
	}

	@Override
	public String toString() {
		return dag.toString();
	}
}
