package broadcast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import Logger.ProcessLogger;
import datastructures.Message;
import datastructures.MessageType;
import streamlet.Node;

public class BroadcastManager {

	private Message lastMessage = null;
	private List<Message> lastMessages = new ArrayList<Message>();
	private int broadcasterId; // own ID

	private Logger processLogger = ProcessLogger.logger;
	
	public BroadcastManager(int broadcasterId) {
		this.broadcasterId = broadcasterId;
	}

	public synchronized void receive(HashMap<Integer, ObjectOutputStream> echoNodes, Message m) throws IOException {
		
		processLogger.info("-------------- BroadcastManager.receive() --------------");
		System.out.println("-------------- BroadcastManager.receive() --------------");
		processLogger.info("Analysing received message...");
		System.out.println("Analysing received message...");
		
		// retrieve inner message (perguntar prof)
		if (m.getMessageType().equals(MessageType.ECHO)) {
			processLogger.info("Message type is ECHO. Retrieving inner message...");
			System.out.println("Message type is ECHO. Retrieving inner message...");
			m = m.getMessage();			
		}

		if (m.getMessageType().equals(MessageType.VOTE)) {
			processLogger.info("Message type is VOTE. Adding to votes...");
			System.out.println("Message type is VOTE. Adding to votes...");
			if (Node.votesReceived.contains(m)) {
				return;
			}
			Node.votesReceived.add(m);
		}
		
		if (lastMessage != null && lastMessage.equals(m)) {
			processLogger.info("Received message is equal to last message skipping echo process");
			System.out.println("Received message is equal to last message skipping echo process");
			return;
		}

		processLogger.info("Received message is new starting echo process...");
		System.out.println("Received message is new starting echo process...");
		Message echoMsg = new Message(MessageType.ECHO, broadcasterId, m, null);

		for (Map.Entry<Integer, ObjectOutputStream> entry : echoNodes.entrySet()) {
			Integer id = entry.getKey();
			ObjectOutputStream stream = entry.getValue();

			if (id == broadcasterId || id == m.getSender()) {
				processLogger.info("Skipping current node with ID " + id +"(same ID as message sender/is this node)");
				System.out.printf("Skipping current node with ID %d(same ID as message sender/is this node)\n", id);
				continue;
			}
			processLogger.info("Echoing message to node with ID "+ entry.getKey());
			System.out.printf("\033[33mEchoing\033[0m message to node with ID %d\n", entry.getKey());

			send(echoMsg, stream);
		}

		if(m.getMessageType().equals(MessageType.PROPOSE)) {
			lastMessage = m;	
		}
		processLogger.info("-------------- BroadcastManager.receive() END --------------");
		System.out.println("-------------- BroadcastManager.receive() END --------------");
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
