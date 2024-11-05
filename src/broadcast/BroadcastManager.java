package broadcast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import datastructures.Message;
import datastructures.MessageType;
import streamlet.Node;

public class BroadcastManager {

	private Message lastMessage = null;
	private List<Message> lastMessages = new ArrayList<Message>();
	private int broadcasterId; // own ID

	public BroadcastManager(int broadcasterId) {
		this.broadcasterId = broadcasterId;
	}

	public synchronized void receive(HashMap<Integer, ObjectOutputStream> echoNodes, Message m) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("-------------- BroadcastManager.receive() --------------\n");
		System.out.println("-------------- BroadcastManager.receive() --------------");
		sb.append("Analysing received message...\n");
		System.out.println("Analysing received message...");
		
		// retrieve inner message (perguntar prof)
		if (m.getMessageType().equals(MessageType.ECHO)) {
			sb.append("Message type is ECHO. Retrieving inner message...\n");
			System.out.println("Message type is ECHO. Retrieving inner message...");
			m = m.getMessage();			
		}

		if (m.getMessageType().equals(MessageType.VOTE)) {
			sb.append("Message type is VOTE. Adding to votes...\n");
			System.out.println("Message type is VOTE. Adding to votes...");
			if (Node.votesReceived.contains(m)) {
				Node.processLogger.write(sb.toString(), "READING THREAD");
				return;
			}
			Node.votesReceived.add(m);
		}
		
		if (lastMessage != null && lastMessage.equals(m)) {
			sb.append("Received message is equal to last message skipping echo process\n");
			System.out.println("Received message is equal to last message skipping echo process");
			Node.processLogger.write(sb.toString(), "READING THREAD");
			return;
		}

		sb.append("Received message is new starting echo process...\n");
		System.out.println("Received message is new starting echo process...");
		Message echoMsg = new Message(MessageType.ECHO, broadcasterId, m, null);

		for (Map.Entry<Integer, ObjectOutputStream> entry : echoNodes.entrySet()) {
			Integer id = entry.getKey();
			ObjectOutputStream stream = entry.getValue();

			if (id == broadcasterId || id == m.getSender()) {
				sb.append("Skipping current node with ID " + id +"(same ID as message sender/is this node)\n");
				System.out.printf("Skipping current node with ID %d(same ID as message sender/is this node)\n", id);
				continue;
			}
			sb.append("\033[33mEchoing\033[0m message to node with ID "+ entry.getKey()+"\n");
			System.out.printf("\033[33mEchoing\033[0m message to node with ID %d\n", entry.getKey());

			send(echoMsg, stream);
		}

		lastMessage = m;
		sb.append("-------------- BroadcastManager.receive() END --------------");
		System.out.println("-------------- BroadcastManager.receive() END --------------");
		Node.processLogger.write(sb.toString(), "READING THREAD");
	}

	public void send(Message m, ObjectOutputStream stream) throws IOException {
		if (m == null) {
			System.out.println("Did not send message because m was null");
			return;
		}
		stream.writeObject(m);
		stream.reset();
	}

	public Message deliver() {
		return lastMessage;
	}
}
