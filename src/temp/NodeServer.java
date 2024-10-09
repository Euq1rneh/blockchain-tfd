package temp;

import java.io.*;
import java.net.*;

public class NodeServer implements Runnable {
    private int port;

    public NodeServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Node listening on port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String receivedMessage;
            while ((receivedMessage = in.readLine()) != null) {
                System.out.println("Received message: " + receivedMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

