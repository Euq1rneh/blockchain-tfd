package network;


import java.io.*;
import java.net.*;
import java.util.HashMap;

import broadcast.BroadcastManager;

public class NodeServer implements Runnable {
	private int port;
    private BroadcastManager bm;
    private HashMap<Integer, ObjectOutputStream> connectedNodes;

    public NodeServer(String selfAddress, HashMap<Integer, ObjectOutputStream> connectedNodes, BroadcastManager bm) {
        this.port = Integer.parseInt(selfAddress.split(":")[1]);        
    	this.connectedNodes = connectedNodes;
    	this.bm = bm;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Node listening on port: " + port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, bm, connectedNodes)).start();
            }
            
            System.out.println("Server socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
