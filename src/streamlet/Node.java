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
import java.util.HashSet;
import java.util.Set;

public class Node {
    private static Set<String> connectedNodes = new HashSet<>();

    private static String[] addresses;
    private static int epochDurationSec;
    private static int seed;
    private static int id;
    
    private static String selfServerAddress;
    
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
        	System.out.println("Usage: java Node <configFilePath> <peersFilePath> <nodeId>");
//            System.out.println("Usage: java Node <selfPort> [<targetHost:targetPort>]");
            return;
        }

        String configFilePath = args[0];
        String peersFilePath = args[1];
        id = Integer.parseInt(args[2]);

        readConfig(configFilePath, peersFilePath);
        
//        int selfPort = Integer.parseInt(args[0]);
//        String selfAddress = "127.0.0.1:" + selfPort; // Test local host needs to change
//
//        connectedNodes.add(selfAddress);
//        System.out.println("\n**Main**");
//        printConnectedNodes();

        NodeServer server = new NodeServer(selfAddress, connectedNodes);
        Thread serverThread = new Thread(server);
        serverThread.start();

        // If on connection mode
        if (args.length > 1) {
            String[] targetInfo = args[1].split(":");
            if (targetInfo.length == 2) {
                String targetHost = targetInfo[0];
                int targetPort = Integer.parseInt(targetInfo[1]);

                // Start the client thread to connect to the target node
                connectToNode(targetHost, targetPort, selfAddress);
            } else {
                System.out.println("Invalid target format. Use <targetHost:targetPort>.");
            }
        } else {
            System.out.println("No target node specified. Running in server-only mode.");
        }
    }
    
    private static int readConfig(String configName, String peersName) {
    	File configFile = new File(configName);
    	File peersFile = new File(peersName);
    	
    	if(!configFile.exists()) {
    		System.out.println("No config file was found with name " + configName);
    		return -1;
    	}
    	
    	if(!peersFile.exists()) {
    		System.out.println("No peer file was found with name " + peersName);
    		return -1;
    	}
    	
    	try {
			BufferedReader br = new BufferedReader(new FileReader(configFile));
			String line;
			while((line = br.readLine()) != null) {
				String[] args = line.split("=");
				
				switch (args[0]) {
				case "seed":
					seed = Integer.parseInt(args[1]);
					break;

				case "epoch_time_sec":
					epochDurationSec = Integer.parseInt(args[1]);
					break;
				}
			}
		} catch (IOException e) {
			System.out.println("Error trying to read config file");
			e.printStackTrace();
		}
    	
    	try {
			BufferedReader br = new BufferedReader(new FileReader(peersFile));
			String line;
			while((line = br.readLine()) != null) {
				String[] args = line.split(" ");
				if(Integer.parseInt(args[0]) == id) {
					selfServerAddress = args[1] + ":" + args[2];
				}
				
			}
		} catch (IOException e) {
			System.out.println("Error trying to read config file");
			e.printStackTrace();
		}
    	
    	return 0;
    }

    public static void connectToNode(String host, int port, String selfAddress) {
        String nodeAddress = host + ":" + port;
        if (connectedNodes.contains(nodeAddress)) {
            System.out.println("Already connected to " + nodeAddress);
            return;
        }

        NodeClient client = new NodeClient(host, port, "CONNECT_REQUEST" + " " + selfAddress, connectedNodes);
        Thread clientThread = new Thread(client);
        clientThread.start();

        //connectedNodes.add(nodeAddress);
    }
    
    public static void printConnectedNodes() {
    	System.out.println("\n=================================================\n" + connectedNodes.toString() + "\n=================================================\n");
    }
}
