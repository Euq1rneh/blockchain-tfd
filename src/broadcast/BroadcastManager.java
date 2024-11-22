package broadcast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import Logger.LoggerSeverity;
import Logger.ProcessLogger;
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
			// ProcessLogger.log("Message type is ECHO from " + m.getSender() + ".
			// Retrieving inner message...", LoggerSeverity.INFO);
			m = m.getMessage();
		}

		// Processar mensagens do tipo PROPOSE
		if (m.getMessageType().equals(MessageType.PROPOSE)) {
			if (Node.currentEpochMessage != null) {
				return; // Se já temos uma mensagem para a época atual, ignore
			}

			if (m.getBlock().getEpoch() == Node.currentEpoch && m.getSender() == Node.currentLider) {
				Node.currentEpochMessage = m;
				ProcessLogger.log("PROPOSE message from " + m.getSender() + " received!!!", LoggerSeverity.INFO);
				echoMessage(echoNodes, m);
				Node.vote();
			}
		}

		if (m.getMessageType().equals(MessageType.VOTE)) {
			if (Node.votesReceived.contains(m) || !m.getBlock().equals(Node.currentBlockToVote)) {
				return;
			}
			ProcessLogger.log("VOTE message from " + m.getSender() + " received!!!", LoggerSeverity.INFO);
			Node.votesReceived.add(m);
			echoMessage(echoNodes, m);
			Node.receivedVoteHandler();
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
			System.out.println("Could not send message to a node");
		}
	}
}
