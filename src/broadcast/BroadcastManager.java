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

	public void receive(Socket socket, int socketId, Message m) throws IOException {
		System.out.println("Checking message...");

//		if(m.getMessageType().equals(MessageType.ECHO)) {
//			
//		}else {
//			
//		}
		if (!m.equals(lastMessage)) {
			int senderId = m.getSender();

			if (socketId == senderId || socketId == broadcasterId)
				return;

			Message echoMsg = new Message(MessageType.ECHO, broadcasterId, m, null);
			send(echoMsg, socket);
			System.out.println("Echoing Message");
			return;
		}
		System.out.println("Already received message once. Cancelling echo");
	}

	public void send(Message m, Socket s) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

		out.writeObject(m);
	}
}
