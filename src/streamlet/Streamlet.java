package streamlet;

import java.net.ServerSocket;
import java.util.Scanner;

import network.ConnectionThread;
import network.NetworkManager;
import network.NodeClient;

public class Streamlet {

	public static void main(String[] args) {

		System.out.println("Started Peer");
		
		boolean running = true;
		Scanner sc = new Scanner(System.in);
		
		System.out.print("Server port:");
		String acceptPort = sc.nextLine();
		int port = Integer.parseInt(acceptPort);

		ServerSocket s = NetworkManager.createServerSocket(port);

		// Accepts incoming connections
		ConnectionThread ct = new ConnectionThread(s);
		ct.start();
		
		
		while (running) {
			System.out.println("Waiting for command...");
			String command = sc.nextLine();

			switch (command) {
			case ":c": {
				System.out.print("IP address: ");
				String ip = sc.nextLine();
				System.out.print("Port: ");
				String p = sc.nextLine();
				int outPort = Integer.parseInt(p);
				
				// Send messages to other nodes (client side)
		        Thread clientThread = new Thread(new NodeClient(ip, outPort));
		        clientThread.start();
				
				break;
			}
			case ":q": {
				running = false;
				break;
			}

			}

		}

		sc.close();
	}

}
