package datastructures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BlockChain implements Serializable {

	private static final long serialVersionUID = -5986244309823453661L;

	private HashDag<Integer> dag; // The DAG representing the blockchain
	private HashSet<Block> processedBlocks;
	
	public BlockChain() {
		this.dag = new HashDag<Integer>();
		this.processedBlocks = new HashSet<Block>();
	}

	public void addGenesisBlock(Block genesisBlock) {
		dag.add(genesisBlock.getEpoch());
		processedBlocks.add(genesisBlock);
	}

	public Block findBlock(int epoch) {
		for (Block b : processedBlocks) {
			if(b.getEpoch() == epoch)
				return b;
		}
		return null;
	}
	
	/**
	 * Adds a block to the blockchain
	 * 
	 * @param parentBlock the parent of the block to add
	 * @param newBlock    the new block to add
	 */
	public void addBlock(Integer parentBlockEpoch, Block newBlock) {
		// Add the new block to the DAG, with the parent block pointing to it
		dag.put(parentBlockEpoch, newBlock.getEpoch());
		processedBlocks.add(newBlock);	
	}

	public void union(BlockChain bc) {
		dag = dag.union(bc.getDAG());
	}

	public boolean contains(int epoch) {
		return dag.contains(epoch);
	}

	public boolean resolveBlockchain(List<Integer> finalizedchain) {
		return dag.retainAll(finalizedchain);
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
