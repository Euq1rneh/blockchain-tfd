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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import broadcast.BroadcastManager;
import datastructures.Block;
import datastructures.Message;
import datastructures.MessageType;

public class Node {

	private static int nodeId;
	private static String selfAddress;
	private static int selfPort;
	private static HashMap<Integer, String> allNodes = new HashMap<Integer, String>(); // List of all nodes read from file
	
	private static volatile HashMap<Integer, ObjectOutputStream> nodeConnections = new HashMap<Integer, ObjectOutputStream>();
	private static volatile List<Block> blockChain = new ArrayList<Block>();
	private static volatile List<Message> votesReceived = new ArrayList<Message>();
	private static volatile boolean receivedProposedBlock = false;
	
	
	private static String startTime; // Configured start time for connections
	private static int seed;
	private static int epochDurationSec;
	
	private static int currentEpoch;
	private static int currentLider;
	private static BroadcastManager bm;
	
	private static Random rd;
	
	
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
	            targetTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	                    .parse(todayDate + timeFormat.format(targetTime));

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
		
		for (Integer id : allNodes.keySet()) {
			String nodeAddress = allNodes.get(id);
			
			String[] args = nodeAddress.split(":");
			
			try (Socket s = new Socket(args[0], Integer.parseInt(args[1]))){
				System.out.printf("Connected to %s\n", nodeAddress);
				
				
				nodeConnections.put(id, new ObjectOutputStream(s.getOutputStream()));
			}catch(IOException e) {
				System.out.printf("Could not connect to node with address %s\n", nodeAddress);
			}
		}
	}
	
	private static void startEpoch() throws IOException {
		electLider();
		propose();
		
		while(true);
		// wait for leader multicast
		//vote();
	}
	
	public static int electLider() {
		System.out.println("Electing new Lider...");
		int index = rd.nextInt() %  nodeConnections.size();
		
		int[] keyArray = nodeConnections.keySet().stream()
                .mapToInt(Integer::intValue)
                .toArray();
		
		currentLider = keyArray[index];
		
		System.out.printf("New Lider is %d\n", keyArray[index]);
		return currentLider;
	}

	private static void propose() throws IOException {
		if (currentLider != nodeId) {
			System.out.println("Is not current epoch Lider");
			System.out.println("Waiting for proposed block");
			while(!receivedProposedBlock);
			return;
		}
		currentEpoch++;
		int blockChainSize = blockChain.size();

		Block newBlock = new Block(currentEpoch, blockChainSize + 1, null, blockChain.get(blockChainSize - 1));

		// multicast of newBlock
		Message m = new Message(MessageType.PROPOSE, nodeId, null, newBlock);

		bm.send(m, nodeConnections);
	}

	private static void vote() {
		// receive proposed block from leader
		System.out.println("Waiting to receive necessary votes");
		while (votesReceived.size() < 5 / 2)
			;

		// if received n/2 votes for block notarize it
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
		NodeServer server = new NodeServer(selfAddress, nodeConnections, bm, receivedProposedBlock);
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
