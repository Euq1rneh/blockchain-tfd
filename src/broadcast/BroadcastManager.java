package broadcast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import datastructures.Message;
import datastructures.MessageType;

public class BroadcastManager {

	private Message lastMessage = null;
	private int broadcasterId; // own ID

	public BroadcastManager(int broadcasterId) {
		this.broadcasterId = broadcasterId;
	}

	public void receive(Socket socket, int socketId, HashMap<Integer, ObjectOutputStream> echoNodes, Message m) throws IOException {
		System.out.println("Analysing received message...");
		//retrieve inner message (perguntar prof)
		if(m.getMessageType().equals(MessageType.ECHO)) {
			System.out.println("Message type is ECHO. Retrieving inner message...");
			m = m.getMessage();	
		}
		
		if(lastMessage.equals(m)) {
			System.out.println("Received message is equal to last message skipping echo process");
			return;
		}
		
		System.out.println("Received message is new starting echo process...");
		Message echoMsg = new Message(MessageType.ECHO, broadcasterId, m, null);
		
		for (Map.Entry<Integer, ObjectOutputStream> entry : echoNodes.entrySet()) {
			Integer id = entry.getKey();
			ObjectOutputStream stream = entry.getValue();
			
			if(id == broadcasterId || id == m.getSender()) {
				System.out.println("Skipping current node (same ID as message sender/is this node)");
				continue;
			}
			System.out.printf("Echoing messagt to node with ID %d\n", entry.getKey());
			
			send(echoMsg, stream);
		}
		
		lastMessage = m;
	}

	public void send(Message m, ObjectOutputStream stream) throws IOException {
			stream.writeObject(m);
	}
	
	public Message deliver() {
		return lastMessage;
	}
}
