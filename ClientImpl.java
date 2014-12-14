import java.io.*;
import java.net.*;
import java.util.*;

public class ClientImpl implements Client{
	//maximum packet length
	private final int maxPacketLen = 1024*64;
	
	//minimum packet length
	private final int minPacketLen = 64;

	//actual packet length
	private static int dataLen;
	
	//server hostname
	private static String serverHostname;
	
	//command decides operation
	private static String command;
	
	//packet length sen
	private static int packetLength;

	//the sending interval
	private static int interval;

	//port
	private static int portNum = 20121;
		
	//no ack within timeOut, resend with smaller packet
	private static int timeOut;

	private static String[] cluster = {"head", "node01", "node02", "node03", "node04", "node05", "node06", "node07", "node08", "node09",
					"node10", "node11", "node12", "node13", "node14", "node15", "node16"};

	//all possible commands
	private static String[] commands={"-i", "-l", "-s", "-r","-d","-e","-q","-n"};

	//client socket
	private static DatagramSocket clientSocket; 
	
	public void startUdpSocket() {
		try{
			String serverHost = this.getServerHostname();
			InetAddress serverAddr = InetAddress.getByName(serverHost);
			
			int sigLen = 2;
			//default data length
			this.setPacketLength(128);

			//set the receiving and sending buffer
			byte[] clientBufferIn = new byte[2048];
			byte[] clientSigBufferOut = new byte[sigLen];
			byte[] clientDataBufferOut = new byte[this.getPacketLength()];

			//create a udp socket at client side
			clientSocket = new DatagramSocket(portNum);
			clientSocket.setSoTimeout(8000);
			//record the command option
			String myCommand = this.getCommand();
			clientSigBufferOut = myCommand.getBytes();
			//send command
			DatagramPacket clientPacketOut = new DatagramPacket(clientSigBufferOut, clientSigBufferOut.length, serverAddr, portNum);
			DatagramPacket clientPacketIn = new DatagramPacket(clientBufferIn, 0, clientBufferIn.length);
			clientSocket.send(clientPacketOut);


			//copy a local file to dfs		
			if(myCommand.equals(commands[2])){
				System.out.print("\n");
				//list local files
				System.out.println("Local files:");
				File local = new File("./");
				String files[]=local.list();
				for(int i=0; i<files.length; i++){
					System.out.println(files[i]);
				}
				System.out.println("Enter the file to upload:");

				BufferedReader usrInpu = new BufferedReader(new InputStreamReader(System.in));
				String localFileName = usrInpu.readLine();
				File localFile = new File(localFileName);
				while(!localFile.exists()){
					System.out.println("Error: no such file, enter again: ");
					usrInpu = new BufferedReader(new InputStreamReader(System.in));
					localFileName = usrInpu.readLine();
					localFile = new File(localFileName);
				}

				byte[] fileName = new byte[2048];
				fileName = localFileName.getBytes();
				clientPacketOut = new DatagramPacket(fileName, fileName.length, serverAddr, portNum);
				clientSocket.send(clientPacketOut);

				int numOfBytes = (int)localFile.length();
				
				//use to store data
				byte[] byteArray = new byte[numOfBytes];
				

				System.out.println("This file has " + numOfBytes + " of bytes.");
				FileInputStream localFileIn = null;
				int numOfPackets = (int)( numOfBytes /( this.getPacketLength()) )+1;
				ByteArrayOutputStream byteOutStr = new ByteArrayOutputStream();
				DataOutputStream intOut = new DataOutputStream(byteOutStr);
				intOut.writeInt(numOfPackets);
				byte[] packetNum = byteOutStr.toByteArray();
				clientPacketOut = new DatagramPacket(packetNum, packetNum.length, serverAddr, portNum);
				clientSocket.send(clientPacketOut);


				System.out.println("It will be sent by " + numOfPackets + " packets of length " + this.getPacketLength());
				
				try{
					localFileIn = new FileInputStream(localFile);
					localFileIn.read(byteArray);

					localFileIn.close();
				}catch(FileNotFoundException fne){
					System.out.println("Error: file is not found.");
				}catch(IOException ioe){
					System.out.println("IO Exception.");
				}
				
			 		
				this.setInterval(20);
				System.out.println("Start Sending...");
				if(byteArray.length <= this.getPacketLength()){
					try{	
						clientPacketOut = new DatagramPacket(byteArray, byteArray.length, serverAddr, portNum);
						clientSocket.send(clientPacketOut);
					}catch(IOException ioe){
						System.out.println("IO Exception :" + ioe);
					}
				}else{
					int packetlen = this.getPacketLength();
					byte[] dataPacket = new byte[packetlen];
					for(int i=0; i<numOfPackets; i++){
						if( i == numOfPackets-1){
							System.arraycopy(byteArray, i*packetlen, dataPacket, 0, (byteArray.length % packetlen));
						}else{
							System.arraycopy(byteArray, i*packetlen, dataPacket,0,packetlen );
						}
						try{
							Thread.sleep(interval);
							clientPacketOut = new DatagramPacket(dataPacket, dataPacket.length, serverAddr, portNum);
							clientSocket.send(clientPacketOut);	
					
						}catch(IOException ioe){
							System.out.println("IO Exception :" + ioe);
						}catch(InterruptedException ie){
							System.out.println("Interrupted Exception :" + ie);
						}
					}
				}
				System.out.println("File sent.");


			}	



			//create new file
			if(myCommand.equals(commands[7])){
				System.out.print("\n");				
				System.out.println("Enter the name of file to be created:");

				BufferedReader usrInpu = new BufferedReader(new InputStreamReader(System.in));
				String localFileName = usrInpu.readLine();
				File localFile = new File(localFileName);

				byte[] fileName = new byte[2048];
				fileName = localFileName.getBytes();
				clientPacketOut = new DatagramPacket(fileName, fileName.length, serverAddr, portNum);
				clientSocket.send(clientPacketOut);

				int numOfBytes = (int)localFile.length();
				
				//use to store data
				byte[] byteArray = new byte[numOfBytes];
				

				//System.out.println("This file has " + numOfBytes + " of bytes.");
				FileInputStream localFileIn = null;
				int numOfPackets = (int)( numOfBytes /( this.getPacketLength()) )+1;
				ByteArrayOutputStream byteOutStr = new ByteArrayOutputStream();
				DataOutputStream intOut = new DataOutputStream(byteOutStr);
				intOut.writeInt(numOfPackets);
				byte[] packetNum = byteOutStr.toByteArray();
				clientPacketOut = new DatagramPacket(packetNum, packetNum.length, serverAddr, portNum);
				clientSocket.send(clientPacketOut);


				//System.out.println("It will be sent by " + numOfPackets + " packets of length " + this.getPacketLength());
				
				try{
					localFileIn = new FileInputStream(localFile);
					localFileIn.read(byteArray);

					localFileIn.close();
				}catch(FileNotFoundException fne){
					//System.out.println("Error: file is not found.");
				}catch(IOException ioe){
					System.out.println("IO Exception.");
				}
				
			 		
				this.setInterval(10);
				//System.out.println("Start Sending...");
				if(byteArray.length <= this.getPacketLength()){
					try{	
						clientPacketOut = new DatagramPacket(byteArray, byteArray.length, serverAddr, portNum);
						clientSocket.send(clientPacketOut);
					}catch(IOException ioe){
						System.out.println("IO Exception :" + ioe);
					}
				}else{
					int packetlen = this.getPacketLength();
					byte[] dataPacket = new byte[packetlen];
					for(int i=0; i<numOfPackets; i++){
						if( i == numOfPackets-1){
							System.arraycopy(byteArray, i*packetlen, dataPacket, 0, (byteArray.length % packetlen));
						}else{
							System.arraycopy(byteArray, i*packetlen, dataPacket,0,packetlen );
						}
						try{
							Thread.sleep(interval);
							clientPacketOut = new DatagramPacket(dataPacket, dataPacket.length, serverAddr, portNum);
							clientSocket.send(clientPacketOut);	
					
						}catch(IOException ioe){
							System.out.println("IO Exception :" + ioe);
						}catch(InterruptedException ie){
							System.out.println("Interrupted Exception :" + ie);
						}
					}
				}
				System.out.println("File created.");


			}	



			//list all files in dfs
			if(myCommand.equals(commands[1])){
				System.out.print("\n");
				clientBufferIn = new byte[2048];
				try{
					clientPacketIn = new DatagramPacket(clientBufferIn, 0, clientBufferIn.length);
					clientSocket.receive(clientPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception :" + ioe);
				}
				ByteArrayInputStream byteInStr = new ByteArrayInputStream(clientPacketIn.getData());
				DataInputStream numIn = new DataInputStream(byteInStr);
				int totalFileNum = numIn.readInt();
				System.out.println("There are " + totalFileNum +" files in DFS.");					


				List<String> serverFiles = new ArrayList<String>();
				int count = 0;
				while(count<totalFileNum){
					try{
						clientBufferIn = new byte[2048];
						clientPacketIn = new DatagramPacket(clientBufferIn, 0, clientBufferIn.length);
						clientSocket.receive(clientPacketIn);
						String tmp = new String(clientPacketIn.getData());
						serverFiles.add(tmp);

					}catch(IOException ioe){
						System.out.println("IO Exception :" + ioe);
					}
					count++;
				}
				for(String fileName : serverFiles){
					System.out.println(fileName);
				}
			}
	
			//download a file from dfs
			if(myCommand.equals(commands[3])){
				System.out.print("\n");
				System.out.println("Enter the file to download:");
				BufferedReader usrInpu = new BufferedReader(new InputStreamReader(System.in));
				String remoteFileName = usrInpu.readLine();
				byte[] fileName = new byte[2048];
				fileName = remoteFileName.getBytes();
				clientPacketOut = new DatagramPacket(fileName, fileName.length, serverAddr, portNum);
				clientSocket.send(clientPacketOut);
				clientBufferIn = new byte[2048];

				try{
					clientPacketIn = new DatagramPacket(clientBufferIn, 0, clientBufferIn.length);
					clientSocket.receive(clientPacketIn);
				}catch(IOException ioe){
					System.out.println("IO Exception :" + ioe);
				}

				ByteArrayInputStream byteInStr = new ByteArrayInputStream(clientPacketIn.getData());
				DataInputStream numIn = new DataInputStream(byteInStr);
				int totalPacketNum = numIn.readInt();
				System.out.println("Total number of packets to receive: "+totalPacketNum);

				int count = 0;
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				OutputStream receiveFile = new FileOutputStream("./"+ remoteFileName);
				

				while(true && (count<totalPacketNum)){
					count++;
					clientPacketIn = new DatagramPacket(clientBufferIn, clientBufferIn.length);
					try{
						clientSocket.receive(clientPacketIn);
					}catch(IOException ioe){
						System.out.println("Error when receiving: " + ioe);
					}
					byte[] tmp = clientPacketIn.getData();
					byteStream.write(tmp, 0, this.getPacketLength());
					if( count == totalPacketNum){
						break;
					}
					
				}
				System.out.println("File received.");
				byteStream.writeTo(receiveFile);
				receiveFile.close();
				
				
			}
			//delete a file
			if(myCommand.equals(commands[4])){
				System.out.print("\n");
				System.out.println("Enter the file to delete:");
				BufferedReader usrInpu = new BufferedReader(new InputStreamReader(System.in));
				String fileToDelete = usrInpu.readLine();
				byte[] fileName = new byte[2048];
				fileName = fileToDelete.getBytes();
				
				clientPacketOut = new DatagramPacket(fileName, fileName.length, serverAddr, portNum);
				clientSocket.send(clientPacketOut);
				
			}

			//edit a file
			if(myCommand.equals(commands[5])){
				System.out.print("\n");
				System.out.println("Enter the file to edit:");
				BufferedReader usrInpu = new BufferedReader(new InputStreamReader(System.in));
				String fileToDelete = usrInpu.readLine();
				byte[] fileName = new byte[2048];
				fileName = fileToDelete.getBytes();
				
				clientPacketOut = new DatagramPacket(fileName, fileName.length, serverAddr, portNum);
				clientSocket.send(clientPacketOut);
				
			}

			if(myCommand.equals(commands[6])){
				clientSocket.close();
			}
	 
		}catch(UnknownHostException ue){
			System.out.println("Unknown host: " + ue);
		}catch(IOException ioe){
			System.out.println("IO Exception: " + ioe);
		}
		
	}


	
	
	
	//getter and setters
	public static String getCommand() {
		return command;
	}

	public static void setCommand(String command) {
		ClientImpl.command = command;
	}

	public static void setInterval(int interval){
		ClientImpl.interval = interval;
	}

	public static int getInterval(){
		return interval;
	}


	public static int getTimeOut() {
		return timeOut;
	}



	public static void setTimeOut(int timeOut) {
		ClientImpl.timeOut = timeOut;
	}

	public static Integer getPacketLength() {
		return packetLength;
	}

	public static void setPacketLength(Integer packetLength) {
		ClientImpl.packetLength = packetLength;
	}

	public static String getServerHostname() {
		return serverHostname;
	}

	public static void setServerHostname(String serverHostname) {
		ClientImpl.serverHostname = serverHostname;
	}

	
	

}
