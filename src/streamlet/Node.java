package streamlet;

import network.NodeServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import Logger.LoggerSeverity;
import Logger.ProcessLogger;
import broadcast.BroadcastManager;
import datastructures.Block;
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
	private static HashMap<Integer, List<Block>> forkBlocks = new HashMap<Integer, List<Block>>(); // contains any
																									// blocks originates
																									// from any fork
	private static List<Block> forkHeads = new ArrayList<Block>(); // contains the blocks that originated forks

	private static volatile List<Block> blockChain = new ArrayList<Block>(); // only contains finalized blocks
	public static volatile List<Block> notarizedChain = new ArrayList<Block>(); // only contains notarized blocks

	public static HashMap<Block, List<Integer>> votesForBlock = new HashMap<Block, List<Integer>>();

	// public static volatile boolean canDeliver = false;
	public static volatile boolean close = false;

	// Config variables
	private static String startTime; // Configured start time for connections
	private static int seed;
	private static int roudDurationSec;
	private static int recoveryRounds;
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

				case "recovery_rounds":
					recoveryRounds = Integer.parseInt(args[1]);
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
			currentLider = currentEpoch % nodeStreams.size();
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
			int parentChainSize = notarizedChain.size();

			Random rd = new Random();
			int receiverId = rd.nextInt() % nodeSockets.size();
			int tAmount = rd.nextInt() * 15;

			Transaction[] transactions = new Transaction[1];
			transactions[0] = new Transaction(nodeId, receiverId, tAmount);

			Block newBlock = new Block(currentEpoch, parentChainSize + 1, transactions, notarizedChain,
					notarizedChain.get(parentChainSize - 1));

			// multicast of newBlock
			m = new Message(MessageType.PROPOSE, nodeId, null, newBlock);

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

		if (messageBlock.getLength() <= notarizedChain.size()) {
			ProcessLogger.log("Chain size of proposed block is smaller. Rejecting block...", LoggerSeverity.INFO);
			// no vote
			return;
		}

		Message vote = new Message(MessageType.VOTE, nodeId, null, messageBlock);

		for (Map.Entry<Integer, ObjectOutputStream> entry : nodeStreams.entrySet()) {
//			ProcessLogger.log("Sending Vote message to node with ID " + entry.getKey(), LoggerSeverity.INFO);
			ObjectOutputStream stream = entry.getValue();
			bm.send(vote, stream);
		}

		ProcessLogger.log("Waiting to receive necessary votes", LoggerSeverity.INFO);

	}

	private static void printForks() {
		ProcessLogger.log("=========Printing chains=============", LoggerSeverity.INFO);

		ProcessLogger.log("BlockChain", LoggerSeverity.INFO);
		StringBuilder sb = new StringBuilder();
		for (Block block : blockChain) {
			sb.append(block.getEpoch() + " -> ");
		}
		ProcessLogger.log(sb.toString(), LoggerSeverity.INFO);
		
		sb = new StringBuilder();
		ProcessLogger.log("Fork heads", LoggerSeverity.INFO);
		for (Map.Entry<Integer, List<Block>> entry : forkBlocks.entrySet()) {
			Integer key = entry.getKey();
			List<Block> val = entry.getValue();
			ProcessLogger.log("Blocks originating from head " + key, LoggerSeverity.INFO);
			for (Block block2 : val) {
				sb.append(block2.getEpoch() + ",");
			}
			ProcessLogger.log(sb.toString(), LoggerSeverity.INFO);
		}
		ProcessLogger.log("\n=====================================", LoggerSeverity.INFO);
	}

	private static boolean blockWasProcessed(int blockEpoch) {
		for (Block block : blockChain) {
			if(block.getEpoch() == blockEpoch)
				return true;
		}
		
		for (Block block : forkHeads) {
			if(block.getEpoch() == blockEpoch)
				return true;
		}

		for (Map.Entry<Integer, List<Block>> entry : forkBlocks.entrySet()) {
			List<Block> blockList = entry.getValue();
			for (Block block : blockList) {
				if(block.getEpoch() == blockEpoch)
					return true;
			}
		}

		return false;
	}

	public synchronized static void receivedVoteHandler(Block currentBlockToVote) {

		if (blockWasProcessed(currentBlockToVote.getEpoch())) {
			ProcessLogger.log("Block was already processed. Skipping process-phase", LoggerSeverity.INFO);
			return;
		}

		if (votesForBlock.get(currentBlockToVote).size() <= (int) (nodeStreams.size() / 2)) {
			return; // Não tem votos suficientes, sai
		}

		ProcessLogger.log("Necessary votes received. Notarizing block...", LoggerSeverity.INFO);

		currentBlockToVote.notarize();

		ProcessLogger.log("Processing vote for block from epoch " + currentBlockToVote.getEpoch(), LoggerSeverity.INFO);

		List<Block> parentChain = currentBlockToVote.getParentChain();
		Block parent = parentChain.get(parentChain.size() - 1);

		ProcessLogger.log("Block epoch " + currentBlockToVote.getEpoch(), LoggerSeverity.INFO);
		ProcessLogger.log("Parent epoch " + parent.getEpoch(), LoggerSeverity.INFO);

		if (forkHeads.contains(parent)) {
			ProcessLogger.log("Parent is a fork head", LoggerSeverity.INFO);

			List<Block> blocks = forkBlocks.get(parent.getEpoch());
			if (!blocks.contains(currentBlockToVote)) {
				blocks.add(currentBlockToVote);
			}
		} else {
			boolean inExistingFork = false;
			ProcessLogger.log("Checking forks for existing parent...", LoggerSeverity.INFO);
			for (Map.Entry<Integer, List<Block>> entry : forkBlocks.entrySet()) {
				List<Block> val = entry.getValue();
				if (val.contains(parent) && !val.contains(currentBlockToVote)) {
					ProcessLogger.log("Parent is in an already existing fork. Adding...", LoggerSeverity.INFO);
					val.add(currentBlockToVote);
					inExistingFork = true;
					break;
				}
			}

			if (!inExistingFork) {
				ProcessLogger.log("Adding new fork", LoggerSeverity.INFO);
				if(!forkHeads.contains(currentBlockToVote) && !forkBlocks.containsKey(currentBlockToVote.getEpoch())) {
					forkHeads.add(currentBlockToVote);
					forkBlocks.put(currentBlockToVote.getEpoch(), new ArrayList<Block>());	
				}
			}
		}
		printForks();

		notarizedChain = new ArrayList<Block>(parentChain);
		notarizedChain.add(currentBlockToVote);

		votesForBlock.remove(currentBlockToVote);// no longer need to collect votes for block

//	    ProcessLogger.log("NotarizedChain with lenght: " + notarizedChain.size(), LoggerSeverity.INFO);

		for (Map.Entry<Block, List<Integer>> entry : votesForBlock.entrySet()) {
			Block b = entry.getKey();
			List<Integer> val = entry.getValue();
			ProcessLogger.log("Votes for block from epoch " + b.getEpoch() + "= " + val.size(), LoggerSeverity.INFO);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("⊥");
		for (int i = 1; i < notarizedChain.size(); i++) {
			sb.append(" -> epoch " + notarizedChain.get(i).getEpoch());
		}

		ProcessLogger.log("Not. CHAIN= " + sb.toString(), LoggerSeverity.INFO);
		finalizeChain();

	}

	private synchronized static void finalizeChain() {
		if (notarizedChain.size() - 1 < 3) {
//			ProcessLogger.log("FINALIZE: Could not finalize chain. Not enough blocks", LoggerSeverity.INFO);
			return;
		}

		int index = notarizedChain.size() - 1;
		Block final1 = notarizedChain.get(index);
		index--;
		Block final2 = notarizedChain.get(index);
		index--;
		Block final3 = notarizedChain.get(index);

		if (final1.getEpoch() == (final2.getEpoch() + 1) && final2.getEpoch() == (final3.getEpoch() + 1)) {
			if ((notarizedChain.size() - 1) > blockChain.size()) {
				blockChain = new ArrayList<Block>(); // leave the cleaning to the garbage collector
				blockChain.addAll(notarizedChain);
				blockChain.remove(notarizedChain.size() - 1);

				StringBuilder sb = new StringBuilder();
				sb.append("⊥");
				for (int i = 1; i < blockChain.size(); i++) {
					sb.append(" -> epoch " + blockChain.get(i).getEpoch());
				}
				ProcessLogger.log("FINALIZE: Finalized a new chain:" + sb.toString(), LoggerSeverity.INFO);
				return;
			}

//			StringBuilder sb = new StringBuilder();
//			sb.append("⊥");
//			for (int i = 1; i < notarizedChain.size(); i++) {
//				sb.append(" -> epoch " + notarizedChain.get(i).getEpoch());
//			}
//
//			ProcessLogger.log("FINALIZED: Nothing new to notarize\nCurrent notarized chain:\n" + sb.toString(),
//					LoggerSeverity.INFO);
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("⊥");
		for (int i = 1; i < notarizedChain.size(); i++) {
			sb.append(" -> epoch " + notarizedChain.get(i).getEpoch());
		}

		ProcessLogger.log(
				"FINALIZE: Last three blocks where not from consecutive epochs. Chain was not finalized\nCurrent notarized chain:\n"
						+ sb.toString(),
				LoggerSeverity.INFO);
	}

	public static void answerRecovery(int sender) {

		Message m = new Message(nodeId, blockChain, notarizedChain, currentEpoch + recoveryRounds,
				MessageType.RECOVERY_ANSWER);

		String nodeAddress = allNodes.get(sender);

		String[] args = nodeAddress.split(":");

		try {
			Socket s = new Socket(args[0], Integer.parseInt(args[1]));
			ObjectOutputStream stream = new ObjectOutputStream(s.getOutputStream());

			stream.flush();

			ProcessLogger.log("Connected to " + nodeAddress, LoggerSeverity.INFO);

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

	public synchronized static void receiveRecovery(Message m) {

		if (hasRecovered) {
			ProcessLogger.log("Already received recovery message (SKIPPING)", LoggerSeverity.INFO);
			return;
		}

		blockChain = m.getBlockchain();
		notarizedChain = m.getNotarizechain();
		currentEpoch = m.getEpochNumber();

		// calcular o tempo de inicio de scheduler (Incio + numeroR * TempoE)

		long epochDuration = (2 * roudDurationSec) + 1;
		LocalTime initialStartTime = LocalTime.parse(startTime);
		// ja tem em conta o incremento das rondas de recuperação?????
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
					roundsToRecover += recoveryRounds;
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
		blockChain.add(genesisBlock);
		notarizedChain.add(genesisBlock);

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
