package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

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

				if (m.getMessageType().equals(MessageType.RECOVERY)) {
					Node.recoveryRequest = true;
					Node.answerRecovery(m.getSender());
				}
				
				if (m.getMessageType().equals(MessageType.RECOVERY_ANSWER)) {
					
				}
				
				if(Node.roundsToRecover > 0) {
					continue;
				}
				
				synchronized (messageQueue) {
					messageQueue.offer(m); // Adicionando mensagens à fila
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	// Método para processar mensagens
	public void processMessages() {
		while (!messageQueue.isEmpty()) {
			Message m;
			synchronized (messageQueue) {
				m = messageQueue.poll(); // Remover a mensagem da fila
			}
			try {
				bm.receive(connectedNodes, m); // Processar a mensagem
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Método para iniciar o processamento de mensagens em uma thread separada
	public void startMessageProcessor() {
		Thread messageProcessorThread = new Thread(() -> {
			while (true) {
				processMessages(); // Processa as mensagens
				try {
					Thread.sleep(100); // Pausa de 100ms entre cada ciclo de processamento
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		messageProcessorThread.start();
	}
}
