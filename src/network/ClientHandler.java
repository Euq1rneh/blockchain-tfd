package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String selfAddress;

    public ClientHandler(Socket socket, String selfAddress) {
        this.clientSocket = socket;
        this.selfAddress = selfAddress;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            String request = (String) in.readObject();
            if (request != null && request.startsWith("CONNECT_REQUEST")) {
                String clientAddress = request.split(" ")[1];
                System.out.println("Received connection request from " + clientAddress);

                // Send acknowledgment back to the client
                out.writeObject("OK");
                out.flush();
            } else {
                System.out.println("Received unknown message: " + request);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}