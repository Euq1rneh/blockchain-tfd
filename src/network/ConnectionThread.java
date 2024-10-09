package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import datastructures.Message;

public class ConnectionThread extends Thread {

	private final ServerSocket s;

	public ConnectionThread(ServerSocket s) {
		this.s = s;
	}

	@Override
	public void run() {
		while (!s.isClosed()) {
            try {
                Socket clientSocket = s.accept();

                new Thread(new ClientHandler(clientSocket)).start();
            } catch (SocketException e) {
            	e.printStackTrace();
            }catch (IOException e) {
                System.out.println("Error accepting connection");
                e.printStackTrace();
            }
        }
        System.out.println("Server socket closed");
	}
	
	class ClientHandler implements Runnable {
	    private Socket clientSocket;

	    public ClientHandler(Socket socket) {
	        this.clientSocket = socket;
	    }

	    @Override
	    public void run() {
	        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
	            Message m = (Message) in.readObject();
	            
	            System.out.println("Received message from peer with id " + m.getSender());
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}

}
