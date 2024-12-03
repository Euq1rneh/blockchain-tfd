package streamlet;

import network.NodeServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import Logger.LoggerSeverity;
import Logger.ProcessLogger;
import broadcast.BroadcastManager;
import datastructures.Block;
import datastructures.BlockChain;
import datastructures.Message;
import datastructures.MessageType;
import datastructures.Transaction;

public class Node {

	private static int nodeId;
	private static String selfAddress;
	private static HashMap<Integer, String> allNodes = new HashMap<Integer, String>(); // List of all nodes read from
																						// file
	private static volatile HashMap<Integer, Socket> nodeSockets = new HashMap<Integer, Socket>();
	private static volatile HashMap<Integer, ObjectOutputStream> nodeStreams = new HashMap<Integer, ObjectOutputStream>();

	// Blockchain variables
//	private static Set<Block> processedBlocks = new HashSet<Block>();
	public static BlockChain blockchain = new BlockChain();
	private static List<Integer> finalBlockchain = new ArrayList<Integer>();

	public static HashMap<Integer, List<Integer>> votesForBlock = new HashMap<Integer, List<Integer>>();

	public static volatile boolean close = false;

	// Config variables
	private static String startTime; // Configured start time for connections
	private static int seed;
	private static int roudDurationSec;
	private static int recoveryEpochs;
	public static int confusionStart;
	public static int confusionDuration;

	// Recovery variables
	public static boolean recoveryRequest;
	public static int roundsToRecover;
	public static boolean hasRecovered;
	public static boolean canProcessMessages = true;

	// Protocol variables
	public static int currentEpoch;
	public static int currentLider;

	private static BroadcastManager bm;
	private static Random rd;

	private static boolean shouldEnterRecoveryMode() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		LocalTime time = LocalTime.parse(startTime, formatter); // time in config
		String formattedTime = LocalTime.now().format(formatter); // current time
		LocalTime now = LocalTime.parse(formattedTime, formatter);

		return time.isBefore(now);
	}

	private static void recoverLider(int numberOfRounds) {
		for (int i = 0; i < numberOfRounds; i++) {
			rd.nextInt();
		}
	}

	private static void waitForStartTime() {
		try {
			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			Date targetTime = timeFormat.parse(startTime);

			Date currentTime = new Date();
			SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd ");
			String todayDate = fullDateFormat.format(currentTime);

			targetTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(todayDate + timeFormat.format(targetTime));

			if (targetTime.before(currentTime)) {
				targetTime = new Date(targetTime.getTime() + TimeUnit.DAYS.toMillis(1));
			}

			long waitTime = targetTime.getTime() - currentTime.getTime();
			if (waitTime > 0) {
				ProcessLogger.log("Waiting until start time: " + startTime, LoggerSeverity.INFO);
				TimeUnit.MILLISECONDS.sleep(waitTime);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean readNodesFile(String nodesFile) {
		File file = new File(nodesFile);

		if (!file.exists()) {
			System.out.println("No config file was found with name " + nodesFile);
			return false;
		}

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("//")) {
					continue;
				}
				String[] args = line.split(" ");
				if (Integer.parseInt(args[0]) == nodeId) {
					selfAddress = args[1];

				}
				allNodes.put(Integer.parseInt(args[0]), args[1]);

			}
		} catch (IOException e) {
			System.out.println("Error trying to read nodes file");
			return false;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return true;
	}

	private static boolean readConfigFile(String configFile) {
		File file = new File(configFile);

		if (!file.exists()) {
			System.out.println("No config file was found with name " + configFile);
			return false;
		}

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] args = line.split("=");

				switch (args[0]) {
				case "seed":
					seed = Integer.parseInt(args[1]);
					break;

				case "round_duration_sec":
					roudDurationSec = Integer.parseInt(args[1]);
					break;

				case "start_time":
					startTime = args[1];
					break;

				case "recovery_epochs":
					recoveryEpochs = Integer.parseInt(args[1]);
					break;
				case "confusion_start":
					confusionStart = Integer.parseInt(args[1]);
					break;
				case "confusion_duration":
					confusionDuration = Integer.parseInt(args[1]);
					break;
				}
			}
		} catch (IOException e) {
			System.out.println("Error trying to read config file");
			return false;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	public static void connectToNode() {
		for (Integer id : allNodes.keySet()) {
			String nodeAddress = allNodes.get(id);

			String[] args = nodeAddress.split(":");

			try {
				Socket s = new Socket(args[0], Integer.parseInt(args[1]));
				ObjectOutputStream stream = new ObjectOutputStream(s.getOutputStream());

				stream.flush();

				ProcessLogger.log("Connected to " + nodeAddress, LoggerSeverity.INFO);

				nodeSockets.put(id, s);
				nodeStreams.put(id, stream);
			} catch (IOException e) {
				System.out.printf("Could not connect to node with address %s\n", nodeAddress);
			}
		}
	}

	public static void close() {
		ProcessLogger.log("Closing Node", LoggerSeverity.INFO);
		for (Map.Entry<Integer, Socket> entry : nodeSockets.entrySet()) {
			Socket socket = entry.getValue();
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		close = true;
	}

	private static void startEpoch() throws IOException {
		epochScheduler();
	}

	public static int electLider() {
		ProcessLogger.log("Electing new Lider (available nodes " + nodeStreams.size() + ")...", LoggerSeverity.INFO);

		if (currentEpoch < confusionStart || currentEpoch >= (confusionStart + confusionDuration - 1)) {
			int index = rd.nextInt(100) % nodeStreams.size();

			int[] keyArray = nodeStreams.keySet().stream().mapToInt(Integer::intValue).toArray();

			currentLider = keyArray[index];
		} else {
			int[] keyArray = nodeStreams.keySet().stream().mapToInt(Integer::intValue).toArray();
			currentLider = keyArray[currentEpoch % nodeStreams.size()];
			// Consume random
			rd.nextInt();
		}
		ProcessLogger.log("New Lider is " + currentLider, LoggerSeverity.INFO);

		return currentLider;
	}
	
	private static void propose() {
		Message m;

		if (currentLider != nodeId) {
			ProcessLogger.log("Is not current epoch Lider\n\"Waiting for proposed block\"", LoggerSeverity.INFO);
		} else {
			List<Integer> longestChain = blockchain.findLongestPath();
			int parentChainSize = longestChain.size();
//			int parentChainSize = notarizedChain.size();

			int parentEpoch = 0; // the index of the last block in the parent chain

			if (parentChainSize > 1) {
				parentEpoch = longestChain.get(parentChainSize - 1);
			}

			ProcessLogger.log("\n\n\nLongest chain: " + longestChain.toString() +"\n\n\n", LoggerSeverity.INFO);
			
			ProcessLogger.log("PARENT EPOCH: "+ parentEpoch, LoggerSeverity.INFO);
			//retrieve block from a list of already processed blocks
			Block parent = blockchain.findBlock(parentEpoch);

			ProcessLogger.log("PARENT EPOCH (fetched block): "+ parent.getEpoch(), LoggerSeverity.INFO);
			// set transactions 
			Random rd = new Random();
			int receiverId = rd.nextInt() % nodeSockets.size();
			int tAmount = rd.nextInt() * 15;

			Transaction[] transactions = new Transaction[1];
			transactions[0] = new Transaction(nodeId, receiverId, tAmount);

			// create new block
			Block newBlock = new Block(currentEpoch, parentChainSize + 1, transactions, blockchain,
					parent);

			// create broadcast message
			m = new Message(MessageType.PROPOSE, nodeId, null, newBlock);
			// multicast of newBlock
			for (Map.Entry<Integer, ObjectOutputStream> entry : nodeStreams.entrySet()) {
				ProcessLogger.log("Sending message to node with ID " + entry.getKey(), LoggerSeverity.INFO);
				ObjectOutputStream stream = entry.getValue();

				bm.send(m, stream);

			}
		}
	}

	public static void vote(Message m) throws IOException {

		if (m == null) {
			ProcessLogger.log("Message was null", LoggerSeverity.INFO);
			return;
		}

		Block messageBlock;

		if ((messageBlock = m.getBlock()) == null) {
			System.out.println("Could not vote. Block in message was null");
			return;
		}
		
		if (messageBlock.getLength() <= blockchain.findLongestPath().size()) {
			ProcessLogger.log("Chain size of proposed block is smaller. Rejecting block...", LoggerSeverity.INFO);
			// no vote
			return;
		}

		Message vote = new Message(MessageType.VOTE, nodeId, null, messageBlock);

//		ProcessLogger.log("Voting on block from epoch " + messageBlock.getEpoch(), LoggerSeverity.INFO);
		
		for (Map.Entry<Integer, ObjectOutputStream> entry : nodeStreams.entrySet()) {
//			ProcessLogger.log("Sending Vote message to node with ID " + entry.getKey(), LoggerSeverity.INFO);
			ObjectOutputStream stream = entry.getValue();
			bm.send(vote, stream);
		}

		ProcessLogger.log("Waiting to receive necessary votes", LoggerSeverity.INFO);

	}

	private static boolean blockWasProcessed(int blockEpoch) {
		return blockchain.contains(blockEpoch);
	}

	public synchronized static void receivedVoteHandler(Block currentBlockToVote) {

		if (blockWasProcessed(currentBlockToVote.getEpoch())) {
			ProcessLogger.log("Block was already processed. Skipping process-phase", LoggerSeverity.INFO);
			return;
		}

		ProcessLogger.log("Checking vote count for block from epoch " + currentBlockToVote.getEpoch(), LoggerSeverity.INFO);
		if (votesForBlock.get(currentBlockToVote.getEpoch()).size() <= (int) (nodeStreams.size() / 2)) {
			ProcessLogger.log("Not enough votes yet...", LoggerSeverity.INFO);
			return; // Não tem votos suficientes, sai
		}

		ProcessLogger.log("Necessary votes received. Notarizing block...", LoggerSeverity.INFO);

		currentBlockToVote.notarize();

		BlockChain parentChain = currentBlockToVote.getBlockChain();
		
		int parent = currentBlockToVote.getParentBlockEpoch();
		ProcessLogger.log("Block epoch " + currentBlockToVote.getEpoch(), LoggerSeverity.INFO);
		ProcessLogger.log("Parent epoch " + parent, LoggerSeverity.INFO);

//		blockchain = parentChain;
		blockchain.union(parentChain);
		blockchain.addBlock(parent, currentBlockToVote);

		System.out.println("Blockchain State\n" + blockchain.toString());

//		processedBlocks.add(currentBlockToVote);
		votesForBlock.remove(currentBlockToVote.getEpoch());// no longer need to collect votes for block

		finalizeBlockchain();
	}

	private static void logToFile() {
		 try (BufferedWriter writer = new BufferedWriter(new FileWriter("blockchain_log-"+nodeId+".txt", true))) {
	            // Loop through the first 10 blocks (or fewer if there aren't 10)
	            int blockCount = Math.min(10, finalBlockchain.size());
	            for (int i = 0; i < blockCount; i++) {
	                int epoch = finalBlockchain.get(0);
	                String epochString = (epoch == 0) ? "⊥" : String.valueOf(epoch);
	                writer.write("epoch " + epochString + " -> ");
	                finalBlockchain.remove(0);
	            }
	        } catch (IOException e) {
	        	
	        }
	}
	
	private static void updatechain(List<Integer> longestchain) {
		boolean canAdd = false;
		for (Integer block : longestchain) {
			if(!canAdd && block == finalBlockchain.get(finalBlockchain.size()-1)) {
				canAdd = true;
				continue;
			}
			
			if(canAdd) {
				finalBlockchain.add(block);
			}
		}
		
	}
	
	private synchronized static void finalizeBlockchain() {
		List<Integer> longestChain = blockchain.findLongestPath();
		int index = longestChain.size() - 1;

		if (longestChain.size() - 1 < 3) {
			return;
		}

		// e.g [..., 1, 2, 3]
		int final1 = longestChain.get(index); // 3
		index--;
		int final2 = longestChain.get(index); // 2
		index--;
		int final3 = longestChain.get(index); // 1

		System.out.println();
		System.out.println();
		ProcessLogger.log("Last 3 blocks: " + final3 + "," + final2 + "," + final1, LoggerSeverity.INFO);
		ProcessLogger.log(longestChain.toString(), LoggerSeverity.INFO);
		System.out.println();
		System.out.println();

		if (final1 == (final2 + 1) && final2 == (final3 + 1)) {
			if ((longestChain.size() - 1) > finalBlockchain.size()) {
				blockchain.resolveBlockchain(longestChain);
				updatechain(longestChain);

				finalBlockchain.remove(finalBlockchain.size()-1);
				
//				StringBuilder sb = new StringBuilder();
//				sb.append("⊥");
//				for (int i = 1; i < finalBlockchain.size(); i++) {
//					sb.append(" -> epoch " + finalBlockchain.get(i));
//				}
				ProcessLogger.log("Finalized a new chain", LoggerSeverity.INFO);
				
				
				if((finalBlockchain.size() / 2 )> 10) {
					ProcessLogger.log("Excess of 10 blocks. Log...", LoggerSeverity.INFO);
					System.out.println(finalBlockchain.toString());
					logToFile();
					System.out.println(finalBlockchain.toString());
				}
				
				return;
			}
			// nothing new to notarize
			ProcessLogger.log("FINALIZED: Nothing new to notarize", LoggerSeverity.INFO);
//			System.out.println("-------->LONGEST CHAIN= " + longestChain.toString());
//			System.out.println("-------->FINAL BLOCKCHAIN= " + finalBlockchain.toString());
			return;
		}

		ProcessLogger.log("Last three blocks where not from consecutive epochs. Chain was not finalized",
				LoggerSeverity.INFO);
	}

	public static void answerRecovery(int sender) {

		Message m = new Message(nodeId, finalBlockchain, blockchain,
				MessageType.RECOVERY_ANSWER);

		String nodeAddress = allNodes.get(sender);

		String[] args = nodeAddress.split(":");

		try {
			Socket s = new Socket(args[0], Integer.parseInt(args[1]));
			ObjectOutputStream stream = new ObjectOutputStream(s.getOutputStream());

			stream.flush();

			ProcessLogger.log("Reconnected to " + nodeAddress, LoggerSeverity.INFO);

			nodeSockets.put(sender, s);
			nodeStreams.put(sender, stream);
		} catch (IOException e) {
			System.out.printf("Could not connect to node with address %s\n", nodeAddress);
		}

		ProcessLogger.log("Sending recovery answer to node with ID " + sender, LoggerSeverity.INFO);
		bm.send(m, nodeStreams.get(sender));
	}

	private static void sendRecovery() {

		Message m = new Message(MessageType.RECOVERY, nodeId);

		for (Map.Entry<Integer, ObjectOutputStream> entry : nodeStreams.entrySet()) {
			if (entry.getKey() == nodeId) {
				continue;
			}
			ProcessLogger.log("Sending recovery message to node with ID " + entry.getKey(), LoggerSeverity.INFO);
			ObjectOutputStream stream = entry.getValue();

			bm.send(m, stream);
		}
	}

	private static int calculateEpoch() {
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	    LocalTime time = LocalTime.parse(startTime, formatter); 
	    LocalTime now = LocalTime.now();
	    long secondsElapsed = java.time.Duration.between(time, now).getSeconds();
	    
	    int epochDuration = (roudDurationSec * 2) + 1; 
	    
	    return (int) (secondsElapsed / epochDuration);
	}
	
	public synchronized static void receiveRecovery(Message m) {

		if (hasRecovered) {
			ProcessLogger.log("Already received recovery message (SKIPPING)", LoggerSeverity.INFO);
			return;
		}

		finalBlockchain = m.getFinalizedChain();
		blockchain = m.getBlockchain();
		currentEpoch = calculateEpoch() + recoveryEpochs; //calculates the current epoch and adds aditional recovery rounds

		System.out.printf("\n\nCalculated epoch %d\n\n", currentEpoch);
		// calcular o tempo de inicio de scheduler (Incio + numeroR * TempoE)

		long epochDuration = (2 * roudDurationSec) + 1;
		LocalTime initialStartTime = LocalTime.parse(startTime);
		Duration totalIncrement = Duration.ofSeconds(epochDuration * currentEpoch);

		LocalTime newStartTime = initialStartTime.plus(totalIncrement);
		startTime = newStartTime.toString();

		hasRecovered = true;

		recoverLider(currentEpoch);
		waitForStartTime();

		canProcessMessages = true;

		try {
			startEpoch();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void epochScheduler() {
		Timer timer = new Timer();

		// Schedule a task to run each epoch
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (recoveryRequest) {
					roundsToRecover += recoveryEpochs;
					recoveryRequest = false;
				}
				if (roundsToRecover > 0) {
					roundsToRecover--;
				}

				currentEpoch++;

				ProcessLogger.log("\n###########################################\n" + " STARTING EPOCH " + currentEpoch
						+ "\n###########################################", LoggerSeverity.INFO);

				electLider();
				propose();
			}
		}, 0, (roudDurationSec * 2 + 1) * 1000); // Epoch duration: 2 * roundDurationSec (one for each round)

		// Optional: Add shutdown hook to stop the timer when the program exits
		Runtime.getRuntime().addShutdownHook(new Thread(timer::cancel));
	}

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Usage: java Node <id> <nodesFile> <configFile>");
			return;
		}

		nodeId = Integer.parseInt(args[0]);
		String nodesFile = args[1];
		String configFile = args[2];

		if (!readNodesFile(nodesFile) || !readConfigFile(configFile)) {
			return;
		}

		rd = new Random(seed);

		Block genesisBlock = new Block(0, 0, null, null, null);
		finalBlockchain.add(0);
		blockchain.addGenesisBlock(genesisBlock);
		
		bm = new BroadcastManager(nodeId);
		try {
			ProcessLogger.setupLogger("process-log-node" + nodeId);
			// ErrorLogger.setupLogger("error-log-node" + nodeId);
		} catch (IOException e) {
			System.out.println("Could not create process/error logger");
		}

		NodeServer server = new NodeServer(selfAddress, nodeStreams, bm);
		Thread serverThread = new Thread(server);
		serverThread.start();

		System.out.printf("Node ID: %d\n", nodeId);

		if (shouldEnterRecoveryMode()) {
			canProcessMessages = false;
			connectToNode();
			// send recovery message
			sendRecovery();
		} else {
			waitForStartTime();
			connectToNode();
			try {
				startEpoch();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
