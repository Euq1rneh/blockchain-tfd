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

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BroadcastManager bm;
    private HashMap<Integer, ObjectOutputStream> connectedNodes;
    private boolean receivedProposedBlock;
    
    public ClientHandler(Socket socket, BroadcastManager bm, HashMap<Integer, ObjectOutputStream> connectedNodes, boolean receivedProposedBlock) {
        this.clientSocket = socket;
        this.bm = bm;
        this.connectedNodes = connectedNodes;
        this.receivedProposedBlock = receivedProposedBlock;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
        	
        	while(true) {
        		Message m = (Message) in.readObject();
            	
            	bm.receive(clientSocket, getSocketId(), connectedNodes, m);	
            	receivedProposedBlock = true;
        	}
        	
        	
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private int getSocketId() {
    	for (Map.Entry<Integer, ObjectOutputStream> entry : connectedNodes.entrySet()) {
			Integer key = entry.getKey();
			ObjectOutputStream val = entry.getValue();
			
			if(clientSocket.equals(val)) {
				return key;
			}
		}
    	
    	return -1;
    }
    
}