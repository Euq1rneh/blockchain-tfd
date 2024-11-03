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

	public synchronized void receive(HashMap<Integer, ObjectOutputStream> echoNodes, Message m) throws IOException {
		System.out.println("-------------- BroadcastManager.receive() --------------");
		System.out.println("Analysing received message...");
		// retrieve inner message (perguntar prof)
		if (m.getMessageType().equals(MessageType.ECHO)) {
			System.out.println("Message type is ECHO. Retrieving inner message...");
			m = m.getMessage();
		}

		if (lastMessage != null && lastMessage.equals(m)) {
			System.out.println("Received message is equal to last message skipping echo process");
			return;
		}

		System.out.println("Received message is new starting echo process...");
		Message echoMsg = new Message(MessageType.ECHO, broadcasterId, m, null);

		for (Map.Entry<Integer, ObjectOutputStream> entry : echoNodes.entrySet()) {
			Integer id = entry.getKey();
			ObjectOutputStream stream = entry.getValue();

			if (id == broadcasterId || id == m.getSender()) {
				System.out.printf("Skipping current node with ID %d(same ID as message sender/is this node)", id);
				continue;
			}
			System.out.printf("\033[33mEchoing\033[0m message to node with ID %d\n", entry.getKey());

			send(echoMsg, stream);
		}

		lastMessage = m;
		System.out.println("-------------- BroadcastManager.receive() END --------------");
	}

	public void send(Message m, ObjectOutputStream stream) throws IOException {
		if (m == null) {
			System.out.println("Did not send message because m was null");
			return;
		}
		stream.writeObject(m);
	}

	public Message deliver() {
		return lastMessage;
	}
}
