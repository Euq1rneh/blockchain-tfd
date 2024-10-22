package network;

import streamlet.Node;

import java.io.*;
import java.net.*;
import java.util.Set;

public class NodeClient implements Runnable {
    private String host;
    private int port;
    private String message;
    private String selfAddress;
    private Set<String> connectedNodes;

    public NodeClient(String address, String message) {
    	String[] aux = address.split(":");
        this.host = aux[0];
        this.port = Integer.parseInt(aux[1]);
        this.message = message;
        this.selfAddress = message.split(" ")[1];
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            //(CONNECT_REQUEST + " " + selfAddress)
            out.writeObject(message);
            out.flush();

            String response = (String) in.readObject();
            if ("OK".equals(response)) {
                System.out.println("Connected to " + host + ":" + port);
            } else {
                System.out.println("Failed to connect to " + host + ":" + port);
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
