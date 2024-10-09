package network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import datastructures.Message;
import datastructures.MessageType;

public class NodeClient implements Runnable {
    private String host;
    private int port;

    public NodeClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
        	System.out.printf("------------------ Connected to %s %d\n -----------------------", host, port);
            Message m = new Message(MessageType.ECHO, null, 1);
        	out.writeObject(m);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
