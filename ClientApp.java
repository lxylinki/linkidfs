
public class ClientApp {

	public static void main(String[] args) {		
		if(args.length != 3){
			System.out.println("Usage: java ClientApp -server hostname -option");
			System.out.println("\t -i \t Initialize DFS");
			System.out.println("\t -l \t List all files");
			System.out.println("\t -s \t Upload");
			System.out.println("\t -r \t Download");
			System.out.println("\t -d \t Delete file");
			System.out.println("\t -n \t Create new file");
			System.out.println("\t -e \t Edit text file");
			System.out.println("\t -q \t Close the server");
			System.exit(1);
		}		
		
		//create a client instance
		ClientImpl myClient = new ClientImpl();
		myClient.setServerHostname(args[1]);
		myClient.setCommand(args[2]);
		//System.out.println("Client command: "+myClient.getCommand());
		myClient.startUdpSocket();
		
	}

}
