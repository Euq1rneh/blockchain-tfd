package streamlet;

import network.NodeClient;
import network.NodeServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Node {

	private static int nodeId;
	private static String selfAddress;
	private static int selfPort;
	private static Set<String> connectedNodes = new HashSet<>(); // Set of connected nodes as "id ip:port"
	private static HashMap<Integer, String> allNodes = new HashMap<Integer, String>(); // List of all nodes read from
																						// file
	private static String startTime; // Configured start time for connections
	private static int seed;
	private static int epochDurationSec;

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

		NodeServer server = new NodeServer(selfAddress);
		Thread serverThread = new Thread(server);
		serverThread.start();

		waitForStartTime();

		// Start the client thread to connect to the target node
		connectToNode();
	}

	private static void waitForStartTime() {
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date targetTime = dateFormat.parse(startTime);
			Date currentTime = new Date();

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
			NodeClient client = new NodeClient(nodeAddress, "CONNECT_REQUEST" + " " + selfAddress);
			Thread clientThread = new Thread(client);
			clientThread.start();
		}
		


		// connectedNodes.add(nodeAddress);
	}

}
