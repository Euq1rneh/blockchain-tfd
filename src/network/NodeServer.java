package network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import broadcast.BroadcastManager;
import streamlet.Node;

public class NodeServer implements Runnable {
    private int port;
    private BroadcastManager bm;
    private HashMap<Integer, ObjectOutputStream> connectedNodes;

    public NodeServer(String selfAddress, HashMap<Integer, ObjectOutputStream> connectedNodes, BroadcastManager bm) {
        this.port = Integer.parseInt(selfAddress.split(":")[1]);
        this.connectedNodes = connectedNodes;
        this.bm = bm;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Node listening on port: " + port);
            while (!Node.close) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, bm, connectedNodes);
                // Inicia o processamento de mensagens em uma thread separada
                clientHandler.startMessageProcessor();  
                new Thread(clientHandler).start();  // Inicia o listener de mensagens de clientes
            }

            System.out.println("Server socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
