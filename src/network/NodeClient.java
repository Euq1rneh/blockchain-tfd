package network;

import streamlet.Node;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import broadcast.BroadcastManager;
import datastructures.Block;
import datastructures.Message;
import datastructures.MessageType;

public class NodeClient implements Runnable {
	private String host;
	private int port;

	private int nodeId; // passado
	private int connectedPeerId;
	private int currentLider;
	private int currentEpoch;

	private int epochDuration; // passado

	private final Random rd;// passado
	private BroadcastManager bm;

	private HashMap<Integer, Socket> connectedNodes; // passado
	private List<Block> blockChain; // partilhado
	private List<Message> votesReceived; // partilhado | tem que ser limpo a cada epoca

	public NodeClient(String address, int nodeId, int epochDuration, int connectedPeerId, Random rd,
			HashMap<Integer, Socket> connectedNodes, List<Block> blockChain, List<Message> votesReceived) {

		String[] aux = address.split(":");
		this.host = aux[0];
		this.port = Integer.parseInt(aux[1]);

		this.nodeId = nodeId;
		this.connectedPeerId = connectedPeerId;
		this.epochDuration = epochDuration;

		this.rd = rd;
		this.connectedNodes = connectedNodes;
		this.blockChain = blockChain;
		this.votesReceived = votesReceived;

		bm = new BroadcastManager(nodeId);
	}

	@Override
	public void run() {
		try (Socket socket = new Socket(host, port);

				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

			connectedNodes.put(connectedPeerId, socket);

			while (connectedNodes.size() < 5)
				;

			// tem que ser loop infinito e depende da duraçao de uma epoch
			startEpoch();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int electLider() {
		System.out.println("Electing new Lider...");
		// may cause problems is a process crashes because of the id's
		currentLider = rd.nextInt() % connectedNodes.size();
		System.out.printf("New Lider is %d\n", currentLider);
		return currentLider;
	}

	private void startEpoch() throws IOException {
		electLider();
		propose();
		// wait for leader multicast
		vote();
	}

	private void propose() throws IOException {
		if (currentLider != nodeId) {
			return;
		}
		currentEpoch++;
		int blockChainSize = blockChain.size();

		Block newBlock = new Block(currentEpoch, blockChainSize + 1, null, blockChain.get(blockChainSize - 1));

		// multicast of newBlock
		Message m = new Message(MessageType.PROPOSE, nodeId, null, newBlock);

		Socket socket = connectedNodes.get(connectedPeerId);
		bm.send(m, socket);
	}

	private void vote() {
		// receive proposed block from leader
		System.out.println("Waiting to receive necessary votes");
		while (votesReceived.size() < 5 / 2)
			;

		// if received n/2 votes for block notarize it
	}
}
