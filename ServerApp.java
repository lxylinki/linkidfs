
import java.io.*;
import java.util.*;

public class ServerApp {
	
	public static void main(String[] args) {		
		if(args.length != 4){
			System.out.println("Usage: java ServerApp -port portnumber -mount directory\n");
			System.out.println("Example: java ServerApp -port 20121 -mount myHomeFolder\n");
			System.exit(1);
		}
		
		ServerImpl myServer = new ServerImpl();
		
		//args[3] = "HelloMyDFS";
		int inputPort = Integer.parseInt(args[1]);
		myServer.setServerPort(inputPort);

		myServer.setHomeFolder("/tmp/"+args[3]);		
		myServer.createHomeFolder(args[3]);
		//update alive servers periodically        
	        myServer.collectAliveServers();
		myServer.startUdpSocket();

	}

}
