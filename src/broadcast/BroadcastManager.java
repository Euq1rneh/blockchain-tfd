package broadcast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Logger.LoggerSeverity;
import Logger.ProcessLogger;
import datastructures.Block;
import datastructures.Message;
import datastructures.MessageType;
import streamlet.Node;

public class BroadcastManager {

	private int broadcasterId;

	public BroadcastManager(int broadcasterId) {
		this.broadcasterId = broadcasterId;
	}

	public void receive(HashMap<Integer, ObjectOutputStream> echoNodes, Message m) throws IOException {

		// Processar mensagens do tipo ECHO
		if (m.getMessageType().equals(MessageType.ECHO)) {
			m = m.getMessage();
		}

		// Processar mensagens do tipo PROPOSE
		if (m.getMessageType().equals(MessageType.PROPOSE)) {
			Block messageBlock;

			if ((messageBlock = m.getBlock()) == null) {
				ProcessLogger.log("Block in propose message was null", LoggerSeverity.INFO);
				return;
			}

			if(Node.notarizedChain.contains(messageBlock)) {
				//ignore propose (likely an echo) block is already notarized
				return;
			}
			
			if (!Node.votesForBlock.containsKey(messageBlock)) {
				Node.votesForBlock.put(messageBlock, new ArrayList<Integer>());
				echoMessage(echoNodes, m);
				Node.vote(m);
			}
		}

		if (m.getMessageType().equals(MessageType.VOTE)) {

			Block messageBlock;

			if ((messageBlock = m.getBlock()) == null) {
				ProcessLogger.log("Block in propose message was null", LoggerSeverity.INFO);
				return;
			}
			
			if(Node.notarizedChain.contains(messageBlock)) {
				//ignore propose (likely an echo) block is already notarized
				return;
			}

			if (Node.votesForBlock.containsKey(messageBlock)) {
				if (Node.votesForBlock.get(messageBlock).contains(m.getSender())) {
					return;
				}
				Node.votesForBlock.get(messageBlock).add(m.getSender());
				echoMessage(echoNodes, m);
				Node.receivedVoteHandler(messageBlock);
			}
		}
	}

	private void echoMessage(HashMap<Integer, ObjectOutputStream> echoNodes, Message m) {
		// Criação de ECHO para retransmissão
		Message echoMsg = new Message(MessageType.ECHO, broadcasterId, m, null);

		for (Map.Entry<Integer, ObjectOutputStream> entry : echoNodes.entrySet()) {
			Integer id = entry.getKey();
			ObjectOutputStream stream = entry.getValue();

			if (id == broadcasterId || id == m.getSender()) {
				continue; // Não enviar para o próprio nó ou para o remetente
			}

			send(echoMsg, stream); // Enviar ECHO para outros nós
		}
	}

	public synchronized void send(Message m, ObjectOutputStream stream) {
		if (m == null) {
			System.out.println("Did not send message because m was null");
			return;
		}

		try {
			stream.writeObject(m);
			stream.flush();
			stream.reset();
		} catch (IOException e) {
			// Node crashed
			System.out.println("Could not send message to a node");
		}
	}
}
