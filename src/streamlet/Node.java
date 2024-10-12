package streamlet;

import network.NodeClient;
import network.NodeServer;

import java.util.HashSet;
import java.util.Set;

public class Node {
    private static Set<String> connectedNodes = new HashSet<>();

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: java Node <selfPort> [<targetHost:targetPort>]");
            return;
        }

        int selfPort = Integer.parseInt(args[0]);
        String selfAddress = "127.0.0.1:" + selfPort; // Test local host needs to change

        connectedNodes.add(selfAddress);
        System.out.println("\n**Main**");
        printConnectedNodes();

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
