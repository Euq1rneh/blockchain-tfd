package network;

import java.net.ServerSocket;

public class NetworkManager {

	public static ServerSocket createServerSocket(int port) {
		
		try(ServerSocket s = new ServerSocket(port)){
			return s;
		}catch (Exception e) {
			return null;
		}
	}
	
}
