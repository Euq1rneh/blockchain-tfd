package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import broadcast.BroadcastManager;
import datastructures.Message;
import datastructures.MessageType;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BroadcastManager bm;
    private HashMap<Integer, Socket> connectedNodes;
    
    public ClientHandler(Socket socket, BroadcastManager bm, HashMap<Integer, Socket> connectedNodes) {
        this.clientSocket = socket;
        this.bm = bm;
        this.connectedNodes = connectedNodes;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

        	Message m = (Message) in.readObject();
        	
        	bm.receive(clientSocket, getSocketId(), m);
        	
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private int getSocketId() {
    	for (Map.Entry<Integer, Socket> entry : connectedNodes.entrySet()) {
			Integer key = entry.getKey();
			Socket val = entry.getValue();
			
			if(clientSocket.equals(val)) {
				return key;
			}
		}
    	
    	return -1;
    }
}