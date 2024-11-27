package test;

import java.util.List;

import datastructures.BlockChain;

public class Test {
	public static void main(String[] args) throws InterruptedException {
		BlockChain bc = new BlockChain();
		
		System.out.println("========== Simple Fork Test ==========");
		simpleFork(bc);
		System.out.println("======================================\n");		
		System.out.println("========== Double Fork Test ==========");
		doubleFork(bc);
		System.out.println("======================================");
	}
	
	private static void simpleFork(BlockChain bc) throws InterruptedException {
		System.out.println("Simulating 2 rounds of confusion at the start of the protocol...");
		bc.addBlock(0, 1);
		bc.addBlock(0, 2);
		Thread.sleep(2000);
		System.out.println("Simulating proposal of block from epoch 3...");
		System.out.println("Deciding on which parent chain 3 should choose...");
		Thread.sleep(2000);
		List<Integer> longestChain = bc.getLongestChain();
		System.out.println("Chain choosen: " + longestChain.toString());
		System.out.println("Adding block to blockchain...");
		bc.addBlock(longestChain.get(longestChain.size() -1 ), 3);
		System.out.println("Simulating finalization...");
		Thread.sleep(2000);
		System.out.println("Final blockchain: " + bc.getLongestChain());
	}
	
	
	private static void doubleFork(BlockChain bc) throws InterruptedException {
		System.out.println("Simulating 2 rounds of confusion at the start of the protocol...");
		bc.addBlock(0, 1);
		bc.addBlock(0, 2);
		Thread.sleep(2000);
		System.out.println("Simulating proposal of block from epoch 3...");
		System.out.println("Deciding on which parent chain 3 should choose...");
		Thread.sleep(2000);
		List<Integer> longestChain = bc.getLongestChain();
		System.out.println("Chain choosen: " + longestChain.toString());
		System.out.println("Adding block to blockchain...");
		bc.addBlock(longestChain.get(longestChain.size() -1 ), 3);
		//2nd round of confusion
		System.out.println("Simulating 3 more rounds of confusion...");
		bc.addBlock(3, 4);
		bc.addBlock(3, 5);
		bc.addBlock(3, 6);
		Thread.sleep(2000);
		
		System.out.println("Simulating proposal of block from epoch 7...");
		System.out.println("Deciding on which parent chain 7 should choose...");
		Thread.sleep(2000);
		longestChain = bc.getLongestChain();
		System.out.println("Chain choosen: " + longestChain.toString());
		
		System.out.println("Adding block to blockchain...");
		bc.addBlock(longestChain.get(longestChain.size() -1 ), 7);
		
		System.out.println("Simulating finalization...");
		Thread.sleep(2000);
		System.out.println("Final blockchain: " + bc.getLongestChain());
	}
}
