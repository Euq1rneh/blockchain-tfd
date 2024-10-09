package temp;

public class Node {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Pass port as argument
        String targetHost = args[1]; // Target node's IP/hostname
        int targetPort = Integer.parseInt(args[2]); // Target node's port

        // Start the server thread to listen for incoming connections
        Thread serverThread = new Thread(new NodeServer(port));
        serverThread.start();

        // Send messages to other nodes (client side)
        Thread clientThread = new Thread(new NodeClient(targetHost, targetPort, "Hello from Node " + port));
        clientThread.start();
    }
}
