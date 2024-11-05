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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
	private static volatile List<Block> blockChain = new ArrayList<Block>();
	public static volatile List<Message> votesReceived = new ArrayList<Message>();

	public static volatile boolean canDeliver = false;
	public static volatile boolean close = false;

	private static String startTime; // Configured start time for connections
	private static int seed;
	private static int epochDurationSec;

	private static int currentEpoch;
	private static int currentLider;
	private static BroadcastManager bm;

	private static Random rd;

	public static ErrorLogger errorLogger;
	public static ProcessLogger processLogger;

	private static void waitForStartTime() {
		try {
			// Parse the time input as "HH:mm:ss"
			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			Date targetTime = timeFormat.parse(startTime);

			// Get the current time in "HH:mm:ss" for today's date
			Date currentTime = new Date();
			SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd ");
			String todayDate = fullDateFormat.format(currentTime);

			// Construct target datetime as "yyyy-MM-dd HH:mm:ss"
			targetTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(todayDate + timeFormat.format(targetTime));

			// If the target time is already passed today, add one day
			if (targetTime.before(currentTime)) {
				targetTime = new Date(targetTime.getTime() + TimeUnit.DAYS.toMillis(1));
			}

			// Calculate wait time
			long waitTime = targetTime.getTime() - currentTime.getTime();
			if (waitTime > 0) {
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

				case "epoch_time_sec":
					epochDurationSec = Integer.parseInt(args[1]);
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
		StringBuilder sb = new StringBuilder();
		for (Integer id : allNodes.keySet()) {
			String nodeAddress = allNodes.get(id);

			String[] args = nodeAddress.split(":");

			try {
				Socket s = new Socket(args[0], Integer.parseInt(args[1]));
				sb.append("Connected to " + nodeAddress + "\n");
				System.out.printf("Connected to %s\n", nodeAddress);

				nodeSockets.put(id, s);
				nodeStreams.put(id, new ObjectOutputStream(s.getOutputStream()));
			} catch (IOException e) {
				System.out.printf("Could not connect to node with address %s\n", nodeAddress);
			}
		}
		processLogger.write(sb.toString(), "MAIN THREAD");
	}

	public static void close() {
		System.out.println("CLOSING NODE");
		for (Map.Entry<Integer, Socket> entry : nodeSockets.entrySet()) {
			Socket socket = entry.getValue();
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		close = true;
	}

	private static void startEpoch() throws IOException {
		electLider();
		Message m = propose();
		// wait for leader multicast
		vote(m);
	}

	public static int electLider() {
		StringBuilder sb = new StringBuilder();
		sb.append("Electing new Lider (available nodes " + nodeStreams.size() + ")...\n");
		System.out.printf("Electing new Lider (available nodes %d)...\n", nodeStreams.size());
		int index = rd.nextInt(100) % nodeStreams.size();

		int[] keyArray = nodeStreams.keySet().stream().mapToInt(Integer::intValue).toArray();

		currentLider = keyArray[index];

		sb.append("New Lider is " + keyArray[index] + "\n");
		System.out.printf("New Lider is %d\n", keyArray[index]);

		processLogger.write(sb.toString(), "MAIN THREAD");
		return currentLider;
	}

	private static Message propose() throws IOException {
		StringBuilder sb = new StringBuilder();
		Message m;
		sb.append("--------------------- PROPOSE PHASE  ---------------------\n");
		System.out.println("--------------------- PROPOSE PHASE  ---------------------");
		if (currentLider != nodeId) {
			sb.append("Is not current epoch Lider\n" + "Waiting for proposed block\n");
			System.out.println("Is not current epoch Lider");
			System.out.println("Waiting for proposed block");
		} else {
			currentEpoch++;
			int blockChainSize = blockChain.size();

			Block newBlock = new Block(currentEpoch, blockChainSize + 1, null, blockChain.get(blockChainSize - 1));

			// multicast of newBlock
			m = new Message(MessageType.PROPOSE, nodeId, null, newBlock);

			for (Map.Entry<Integer, ObjectOutputStream> entry : nodeStreams.entrySet()) {
				sb.append("Sending message to node with ID " + entry.getKey() + "\n");
				System.out.printf("\033[34mSending\033[0m message to node with ID %d\n", entry.getKey());
				ObjectOutputStream stream = entry.getValue();
				bm.send(m, stream);
			}
		}

		while (!canDeliver)
			;
		sb.append("Delivering message\n");
		System.out.println("Delivering message");
		m = bm.deliver();

		if (m == null) {
			sb.append("Error delivering message\n");
			System.out.println("Error delivering message");
			processLogger.write(sb.toString(), "MAIN THREAD");
			return null;
		}
		sb.append("Received message from " + m.getSender() + " of type " + m.getMessageType().toString() + "\n");
		System.out.printf("Received message from %d of type %s\n", m.getSender(), m.getMessageType().toString());
		sb.append("--------------------- PROPOSE PHASE END ---------------------\n");
		System.out.println("--------------------- PROPOSE PHASE END ---------------------");

		processLogger.write(sb.toString(), "MAIN THREAD");
		return m;
	}

	private static void vote(Message m) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("--------------------- VOTE PHASE ---------------------\n");
		System.out.println("--------------------- VOTE PHASE ---------------------");
		// receive proposed block from leader
		Block b = m.getBlock();
		if (b == null) {
			sb.append("Could not vote. Block in message was null\n");
			System.out.println("Could not vote. Block in message was null");

			processLogger.write(sb.toString(), "MAIN THREAD");
			return;
		}

		if (b.getLength() <= blockChain.size()) {
			sb.append("Chain size of proposed block is smaller. Rejecting block...\n");
			System.out.println("Chain size of proposed block is smaller. Rejecting block...");
			// no vote
			processLogger.write(sb.toString(), "MAIN THREAD");
			;
			return;
		}

		Message vote = new Message(MessageType.VOTE, nodeId, null, b);

		for (Map.Entry<Integer, ObjectOutputStream> entry : nodeStreams.entrySet()) {
			sb.append("Voting message to node with ID " + entry.getKey() + "\n");
			System.out.printf("\033[35mVoting\033[0m message to node with ID %d\n", entry.getKey());
			ObjectOutputStream stream = entry.getValue();
			bm.send(vote, stream);
		}

		sb.append("Waiting to receive necessary votes\n");
		System.out.println("Waiting to receive necessary votes");

		while (votesReceived.size() <= (int) (nodeStreams.size() / 2)) {
//			System.out.printf("Votes needed: %d\n", (nodeStreams.size() / 2));
//			System.out.printf("Votes received: %d | Missing votes: %d\n", votesReceived.size(),
//					(nodeStreams.size() / 2) - votesReceived.size());
		}

		// if received n/2 votes for block notarize it
		sb.append("--------------------- VOTE PHASE  END ---------------------\n");
		System.out.println("--------------------- VOTE PHASE  END ---------------------");

		sb.append("Final vote count= " + votesReceived.size() + "\n");
		System.out.println("Final vote count= " + votesReceived.size());
		processLogger.write(sb.toString(), "MAIN THREAD");
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

		Block genesisBlock = new Block(0, 0, null, null);
		blockChain.add(genesisBlock);

		bm = new BroadcastManager(nodeId);
		errorLogger = new ErrorLogger();
		processLogger = new ProcessLogger();

//		errorLogger.createLogFile("error-log-node" + nodeId + ".txt");
//		processLogger.createLogFile("process-log-node" + nodeId + ".txt");
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
