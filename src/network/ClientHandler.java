package network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import Logger.LoggerSeverity;
import Logger.ProcessLogger;
import broadcast.BroadcastManager;
import datastructures.Message;
import datastructures.MessageType;
import streamlet.Node;

class ClientHandler implements Runnable {
	private Socket clientSocket;
	private BroadcastManager bm;
	private HashMap<Integer, ObjectOutputStream> connectedNodes;
	private Queue<Message> messageQueue;

	public ClientHandler(Socket socket, BroadcastManager bm, HashMap<Integer, ObjectOutputStream> connectedNodes) {
		this.clientSocket = socket;
		this.bm = bm;
		this.connectedNodes = connectedNodes;
		this.messageQueue = new LinkedList<>(); // Usando uma fila para armazenar as mensagens
	}

	@Override
	public void run() {
		// Thread para receber mensagens
		try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
			while (true) {
				Message m = (Message) in.readObject();

				synchronized (messageQueue) {
					messageQueue.offer(m); // Adicionando mensagens à fila
				}
			}
		} catch (EOFException e) {
			ProcessLogger.log("Broken node socket", LoggerSeverity.INFO);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	// Método para processar mensagens
	public void processMessages() {
		if (!messageQueue.isEmpty()) {
			if (Node.currentEpoch < Node.confusionStart
					|| Node.currentEpoch >= (Node.confusionStart + Node.confusionDuration - 1)) {
				Message m;
				synchronized (messageQueue) {
					m = messageQueue.poll(); // Remover a mensagem da fila
				}

				if (m.getMessageType().equals(MessageType.RECOVERY)) {

					ProcessLogger.log("Received recovery message from node with ID " + m.getSender(),
							LoggerSeverity.INFO);

					if (!Node.canProcessMessages) {
						ProcessLogger.log("Skipping message recovery request (in RECOVERY MODE)", LoggerSeverity.INFO);
						return;
					}
					
					Node.answerRecovery(m.getSender());
					return;
				}

				if (m.getMessageType().equals(MessageType.RECOVERY_ANSWER)) {
					Node.receiveRecovery(m);
					return;
				}

				try {
					bm.receive(connectedNodes, m); // Processar a mensagem
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Método para iniciar o processamento de mensagens em uma thread separada
	public void startMessageProcessor() {
		Thread messageProcessorThread = new Thread(() -> {
			while (true) {
				processMessages(); // Processa as mensagens
				try {
					Thread.sleep(50); // Pausa de 100ms entre cada ciclo de processamento
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		messageProcessorThread.start();
	}
}
