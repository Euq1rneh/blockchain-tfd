package streamlet;

import network.NodeServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import Logger.ErrorLogger;
import Logger.ProcessLogger;
import broadcast.BroadcastManager;
import datastructures.Block;
import datastructures.Message;
import datastructures.MessageType;

public class Node {

	private static int nodeId;
	private static String selfAddress;
	private static int selfPort;
	private static HashMap<Integer, String> allNodes = new HashMap<Integer, String>(); // List of all nodes read from
																						// file

	private static volatile HashMap<Integer, Socket> nodeSockets = new HashMap<Integer, Socket>();
	private static volatile HashMap<Integer, ObjectOutputStream> nodeStreams = new HashMap<Integer, ObjectOutputStream>();
	private static volatile List<Block> blockChain = new ArrayList<Block>(); // only contains finalized blocks
	private static volatile List<Block> notarizedChain = new ArrayList<Block>(); // only contains notarized blocks
	public static volatile Queue<Message> votesReceived = new ConcurrentLinkedQueue<Message>();

	public static volatile boolean canDeliver = false;
	public static volatile boolean close = false;

	private static String startTime; // Configured start time for connections
	private static int seed;
	private static int roudDurationSec;

	private static int currentEpoch;
	private static int currentLider;
	private static Message currentEpochMessage;
	private static BroadcastManager bm;

	private static Random rd;

	private static Logger processLogger = ProcessLogger.logger;

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
				processLogger.info("Waiting until start time: " + startTime);
				System.out.println("Waiting until start time: " + startTime);
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

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
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
		}
		return true;
	}

	private static boolean readConfigFile(String configFile) {
		File file = new File(configFile);

		if (!file.exists()) {
			System.out.println("No config file was found with name " + configFile);
			return false;
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
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

				}
			}
		} catch (IOException e) {
			System.out.println("Error trying to read config file");
			return false;
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

				processLogger.info("Connected to " + nodeAddress);
				System.out.printf("Connected to %s\n", nodeAddress);

				nodeSockets.put(id, s);
				nodeStreams.put(id, stream);
			} catch (IOException e) {
				System.out.printf("Could not connect to node with address %s\n", nodeAddress);
			}
		}
	}

	public static void close() {
		processLogger.info("Closing Node");
		System.out.println("CLOSING NODE");
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
		finalizationScheduler();
	}

	public static int electLider() {
		processLogger.info("Electing new Lider (available nodes " + nodeStreams.size() + ")...");
		System.out.printf("Electing new Lider (available nodes %d)...\n", nodeStreams.size());
		int index = rd.nextInt(100) % nodeStreams.size();

		int[] keyArray = nodeStreams.keySet().stream().mapToInt(Integer::intValue).toArray();

		currentLider = keyArray[index];

		processLogger.info("New Lider is " + keyArray[index]);
		System.out.printf("New Lider is %d\n", keyArray[index]);

		return currentLider;
	}

	private static Message propose() {
		Message m;
		processLogger.info("--------------------- PROPOSE PHASE  ---------------------");
		System.out.println("--------------------- PROPOSE PHASE  ---------------------");

		if (currentLider != nodeId) {
			processLogger.info("Is not current epoch Lider");
			processLogger.info("Waiting for proposed block");

			System.out.println("Is not current epoch Lider");
			System.out.println("Waiting for proposed block");

		} else {
			int parentChainSize = notarizedChain.size();

			Block newBlock = new Block(currentEpoch, parentChainSize + 1, null, notarizedChain,
					notarizedChain.get(parentChainSize - 1));

			// multicast of newBlock
			m = new Message(MessageType.PROPOSE, nodeId, null, newBlock);

			for (Map.Entry<Integer, ObjectOutputStream> entry : nodeStreams.entrySet()) {
				processLogger.info("Sending message to node with ID " + entry.getKey());
				System.out.printf("\033[34mSending\033[0m message to node with ID %d\n", entry.getKey());
				ObjectOutputStream stream = entry.getValue();

				bm.send(m, stream);

			}
		}

		while (!canDeliver)
			;
		processLogger.info("Delivering message");
		System.out.println("Delivering message");
		m = bm.deliver();

		if (m == null) {
			System.out.println("Error delivering message");
			return null;
		}
		processLogger.info("Received message from " + m.getSender() + " of type " + m.getMessageType().toString()
				+ "\n\n" + "######################\n######################\n" + "NOT-CHAIN-LEN=" + notarizedChain.size()
				+ "\nBLOCK-NOT-CHAIN-LEN=" + m.getBlock().getLength()
				+ "\n######################\n######################\n");
		System.out.printf("Received message from %d of type %s\n", m.getSender(), m.getMessageType().toString());

		processLogger.info("--------------------- PROPOSE PHASE END ---------------------");
		System.out.println("--------------------- PROPOSE PHASE END ---------------------");

		return m;
	}

	private static void vote(Message m) throws IOException {
		processLogger.info("--------------------- VOTE PHASE ---------------------");
		System.out.println("--------------------- VOTE PHASE ---------------------");
		// receive proposed block from leader
		Block b = m.getBlock();

		if (b == null) {
			System.out.println("Could not vote. Block in message was null");
			return;
		}

		if (b.getLength() <= notarizedChain.size()) {
			processLogger.info("Chain size of proposed block is smaller. Rejecting block...");
			System.out.println("Chain size of proposed block is smaller. Rejecting block...");
			// no vote
			return;
		}

		Message vote = new Message(MessageType.VOTE, nodeId, null, b);

		for (Map.Entry<Integer, ObjectOutputStream> entry : nodeStreams.entrySet()) {
			processLogger.info("Sending Vote message to node with ID " + entry.getKey());
			System.out.printf("\033[35mVoting\033[0m message to node with ID %d\n", entry.getKey());
			ObjectOutputStream stream = entry.getValue();
			bm.send(vote, stream);
		}

		processLogger.info("Waiting to receive necessary votes");
		System.out.println("Waiting to receive necessary votes");

		while (votesReceived.size() <= (int) (nodeStreams.size() / 2))
			;

		// if received n/2 votes block is notarize it
		processLogger.info("--------------------- VOTE PHASE  END ---------------------");
		System.out.println("--------------------- VOTE PHASE  END ---------------------");

		processLogger.info("Final vote count= " + votesReceived.size());
		System.out.println("Final vote count= " + votesReceived.size());

		b.notarize();
		// should add or
		// notarizedChain.add(b);
		// should replace????
		notarizedChain.clear();
		notarizedChain.addAll(b.getParentChain());
		notarizedChain.add(b);
	}

	private static void finalizeChain() {
		if(notarizedChain.size() - 1 < 3) {
			processLogger.info("FINALIZE: Could not finalize chain. Not enough blocks");
			System.out.println("FINALIZE: Could not finalize chain. Not enough blocks");
			return;
		}
		
		int index = notarizedChain.size() - 1;
		Block final1 = notarizedChain.get(index);
		index--;
		Block final2 = notarizedChain.get(index);
		index--;
		Block final3 = notarizedChain.get(index);
		
		if(final1.getEpoch() == (final2.getEpoch()+1) && final2.getEpoch() == (final3.getEpoch() + 1)) {
			if((notarizedChain.size() - 1) > blockChain.size()) {
				blockChain.clear();
				blockChain.addAll(notarizedChain);
				blockChain.removeLast();
				
				StringBuilder sb = new StringBuilder();
				sb.append("⊥");
				for (int i = 1; i < blockChain.size(); i++) {
					sb.append(" -> epoch " + blockChain.get(i).getEpoch());
				}
				processLogger.info("FINALIZE: Finalized a new chain:" + sb.toString());
				System.out.println("FINALIZE: Finalized a new chain:" + sb.toString());
				return;
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("⊥");
			for (int i = 1; i < notarizedChain.size(); i++) {
				sb.append(" -> epoch " + notarizedChain.get(i).getEpoch());
			}
			
			processLogger.info("FINALIZED: Nothing new to notarize => " + sb.toString());
			System.out.println("FINALIZED: Nothing new to notarize => " + sb.toString());
			return;
		}
		
		processLogger.info("FINALIZE: Last three blocks where not from consecutive epochs. Chain was not finalized\n");
		System.out.println("FINALIZE: Last three blocks where not from consecutive epochs. Chain was not finalized\n");
	}

	private static void epochScheduler() {
		Timer timer = new Timer();

		// Schedule a task to run each epoch
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				canDeliver = false;
				currentEpoch++;
				votesReceived.clear();

				processLogger.info("###########################################\n" + " STARTING EPOCH " + currentEpoch
						+ "\n###########################################");
				System.out.println("###########################################\n" + " STARTING EPOCH " + currentEpoch
						+ "\n###########################################");

				electLider();
				currentEpochMessage = propose();

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						try {
							vote(currentEpochMessage);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}, roudDurationSec * 1000); // delay equal to roundDurationSec in milliseconds
			}
		}, 0, roudDurationSec * 2 * 1000); // Epoch duration: 2 * roundDurationSec (one for each round)

		// Optional: Add shutdown hook to stop the timer when the program exits
		Runtime.getRuntime().addShutdownHook(new Thread(timer::cancel));
	}

	private static void finalizationScheduler() {
		Timer timer = new Timer();

		// Schedule a task to run every 5 seconds
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				finalizeChain();
			}
		}, 0, roudDurationSec * 1000); // Initial delay of roundDurationSec ms, repeat every roundDurationSec ms

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
		waitForStartTime();

		// Start the client thread to connect to the target node
		connectToNode();

		try {
			startEpoch();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
