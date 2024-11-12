package broadcast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import datastructures.Message;
import datastructures.MessageType;
import streamlet.Node;

public class BroadcastManager {

	//Podemos usar uma message queue para processar as mensagens numa unica thread
	
	private Message lastMessage = null;
	private List<Message> lastMessages = new ArrayList<Message>();
	private int broadcasterId; // own ID
	
	public BroadcastManager(int broadcasterId) {
		this.broadcasterId = broadcasterId;
	}

	public synchronized void receive(HashMap<Integer, ObjectOutputStream> echoNodes, Message m) throws IOException {
		
//		ProcessLogger.log("-------------- BroadcastManager.receive() --------------", LoggerSeverity.INFO);
//		ProcessLogger.log("Analysing received message...", LoggerSeverity.INFO);
		
		// retrieve inner message (perguntar prof)
		if (m.getMessageType().equals(MessageType.ECHO)) {
//			ProcessLogger.log("Message type is ECHO. Retrieving inner message...", LoggerSeverity.INFO);
			m = m.getMessage();			
		}

		if (m.getMessageType().equals(MessageType.VOTE)) {
//			ProcessLogger.log("Message type is VOTE. Adding to votes...", LoggerSeverity.INFO);
			if (Node.votesReceived.contains(m)) {
				return;
			}
			Node.votesReceived.add(m);
		}
		
		if (lastMessage != null && lastMessage.equals(m)) {
//			ProcessLogger.log("Received message is equal to last message skipping echo process", LoggerSeverity.INFO);
			return;
		}

//		ProcessLogger.log("Received message is new starting echo process...", LoggerSeverity.INFO);
		Message echoMsg = new Message(MessageType.ECHO, broadcasterId, m, null);

		for (Map.Entry<Integer, ObjectOutputStream> entry : echoNodes.entrySet()) {
			Integer id = entry.getKey();
			ObjectOutputStream stream = entry.getValue();

			if (id == broadcasterId || id == m.getSender()) {
//				ProcessLogger.log("Skipping current node with ID " + id +"(same ID as message sender/is this node)", LoggerSeverity.INFO);
				continue;
			}
//			ProcessLogger.log("Echoing message to node with ID "+ entry.getKey(), LoggerSeverity.INFO);
			send(echoMsg, stream);
		}

		if(m.getMessageType().equals(MessageType.PROPOSE)) {
			lastMessage = m;	
		}
		
//		ProcessLogger.log("-------------- BroadcastManager.receive() END --------------", LoggerSeverity.INFO);
	}

	public synchronized void send(Message m, ObjectOutputStream stream){
		if (m == null) {
			System.out.println("Did not send message because m was null");
			return;
		}
		
		try {
			stream.writeObject(m);
			stream.flush();
			stream.reset();
		} catch (IOException e) {
			System.out.println("Could not send message to a node");
		}
	}

	public Message deliver() {
		Message deliver = lastMessage;
		lastMessage = null;
		return deliver;
	}
}
