package temp;

import java.io.*;
import java.net.*;

public class NodeClient implements Runnable {
    private String host;
    private int port;
    private String message;

    public NodeClient(String host, int port, String message) {
        this.host = host;
        this.port = port;
        this.message = message;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
            System.out.println("Message sent to " + host + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

