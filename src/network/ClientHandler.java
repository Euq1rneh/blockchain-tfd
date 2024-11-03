package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import broadcast.BroadcastManager;
import datastructures.Block;
import datastructures.Message;
import datastructures.MessageType;
import streamlet.Node;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BroadcastManager bm;
    private HashMap<Integer, ObjectOutputStream> connectedNodes;
    
    public ClientHandler(Socket socket, BroadcastManager bm, HashMap<Integer, ObjectOutputStream> connectedNodes) {
        this.clientSocket = socket;
        this.bm = bm;
        this.connectedNodes = connectedNodes;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());) {
        	
        	while(true) {
        		
        		Message m = (Message) in.readObject();
        		
            	bm.receive(connectedNodes, m);
            	Node.canDeliver = true;
        	}
        	
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}