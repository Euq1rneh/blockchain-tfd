package network;

import streamlet.Node;

import java.io.*;
import java.net.*;
import java.util.Set;

import org.w3c.dom.NamedNodeMap;

public class NodeClient implements Runnable {
    private String host;
    private int port;
    private String message;
    private String selfAddress;
    private Set<String> connectedNodes;

    public NodeClient(String host, int port, String message, Set<String> connectedNodes) {
        this.host = host;
        this.port = port;
        this.message = message;
        this.selfAddress = message.split(" ")[1];
        this.connectedNodes = connectedNodes;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            //(CONNECT_REQUEST + " " + selfAddress)
            out.writeObject(message);
            out.flush();

            String response = (String) in.readObject();
            if ("OK".equals(response)) {
                System.out.println("Connected to " + host + ":" + port);
                this.connectedNodes.add(host + ":" + port);

                // Receive the list of connected nodes
                Set<String> receivedNodes = (Set<String>) in.readObject();
                for (String nodeInfo : receivedNodes) {
                    System.out.println("Discovered node: " + nodeInfo);
                    Node.connectToNode(nodeInfo.split(":")[0], Integer.parseInt(nodeInfo.split(":")[1]), selfAddress);
                }
                System.out.println("\n**Client**");
                Node.printConnectedNodes();
            } else {
                System.out.println("Failed to connect to " + host + ":" + port);
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
