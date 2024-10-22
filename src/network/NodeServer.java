package network;

import streamlet.Node;

import java.io.*;
import java.net.*;
import java.util.Set;

public class NodeServer implements Runnable {
	private int port;
    private String selfAddress;
    private Set<String> connectedNodes;
    

    public NodeServer(String selfAddress) {
        this.port = Integer.parseInt(selfAddress.split(":")[1]);
    	this.selfAddress = selfAddress;        
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Node listening on port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, selfAddress)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
