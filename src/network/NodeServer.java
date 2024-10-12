package network;

import streamlet.Node;

import java.io.*;
import java.net.*;
import java.util.Set;

public class NodeServer implements Runnable {
	private int port;
    private String selfAddress;
    private Set<String> connectedNodes;
    

    public NodeServer(String selfAddress, Set<String> connectedNodes) {
        this.port = Integer.parseInt(selfAddress.split(":")[1]);
    	this.selfAddress = selfAddress;
        this.connectedNodes = connectedNodes;
        
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Node listening on port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, connectedNodes, selfAddress)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Set<String> connectedNodes;
    private String selfAddress;

    public ClientHandler(Socket socket, Set<String> connectedNodes, String selfAddress) {
        this.clientSocket = socket;
        this.connectedNodes = connectedNodes;
        this.selfAddress = selfAddress;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            String request = (String) in.readObject();
            if (request != null && request.startsWith("CONNECT_REQUEST")) {
                String clientAddress = request.split(" ")[1];
                System.out.println("Received connection request from " + clientAddress);

                // Send acknowledgment back to the client
                out.writeObject("OK");
                out.flush();

                // Attempt to connect back to the requesting node
                String[] addressParts = clientAddress.split(":");
                String clientHost = addressParts[0];
                int clientPort = Integer.parseInt(addressParts[1]);

                if (!connectedNodes.contains(clientAddress)) {
                    Node.connectToNode(clientHost, clientPort, selfAddress);
                }

                System.out.println("Established two-way connection with " + clientAddress);
                out.writeObject(connectedNodes);
                out.flush();
                //this.connectedNodes.add(clientAddress);
                System.out.println("\n**Server**");
                Node.printConnectedNodes();
            } else {
                System.out.println("Received unknown message: " + request);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
