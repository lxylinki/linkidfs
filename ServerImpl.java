import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;

public class ServerImpl implements Server{
	//maximum packet length
	private final int maxPacketLen = 1024*64;

	//maximum packet length
	private final int minPacketLen = 64;
	
	//maximum packet length
	private static int packetLength;
	
	//interval
	private static int interval;
		
	// DFS dir: /tmp/homeFolder
	private static String homeFolder;

	// the file name to process
	private static String fileName;
	
	//UDP port
	private static int serverPort;
	
	// active servers from node01 to node16
	private static List<String> aliveNodes = new ArrayList<String>();
	
	// file names list in DFS sync across alive nodes
	private static List<dfsFile> myFiles = new ArrayList<dfsFile>();
	
	// all nodes in the cluster
	private static String[] cluster = 
		{"head", "node01","node02","node03","node04","node05","node06","node07", "node08","node09",
		"node10","node11","node12", "node13","node14","node15","node16"};
	// all acceptable commands
	private static String[] commands ={"-i","-l","-s","-r","-d","-e","-q","-n"};
	
	//set the receiving buffer
	private static byte[] serverBufferIn = new byte[2048];
			
	//set the sending buffer
	private static byte[] serverBufferOut = new byte[2048];

	//port
	private static int myPort;

	private static DatagramSocket serverSocket;
	
	public void startUdpSocket(){
			myPort = this.getServerPort();
			this.setPacketLength(128);
			int sigLen = 2;
		
			String serverSig = "-#";
			byte[] peerSig = new byte[sigLen];
			peerSig = serverSig.getBytes();
		
		try{
			//set the receiving buffer
			//byte[] serverBufferIn = new byte[2048];
			//set the sending buffer
			//byte[] serverBufferOut = new byte[2048];
			InetAddress myAddr = InetAddress.getLocalHost();
			String myHostName = myAddr.getHostName();
			System.out.println("Udp socket is starting on " + myHostName + " at port: " + myPort );
			//create a udp socket at myPort			
			serverSocket = new DatagramSocket(myPort);
			//prepare to receive a packet of length sigLen
			DatagramPacket serverPacketIn = new DatagramPacket(serverBufferIn,0, sigLen);
		
			this.pingPeers();
			String inpuString = "";
			String clientCommand = "";
			
			serverSocket.receive(serverPacketIn);
			inpuString = new String(serverPacketIn.getData());
			clientCommand = inpuString.substring(0,2);
			
			//client info
			InetAddress clientAddr = serverPacketIn.getAddress();
			String clientHostName = clientAddr.getHostName();
		
			while(! (clientCommand.equals(commands[6]))){
	
			//this is actually a peer server hello signal
			while(clientCommand.equals("-#")){
				System.out.print("\n");
				InetAddress peerAddr = serverPacketIn.getAddress();
				String peerHostName = peerAddr.getHostName();
				if(! ((peerHostName+".cluster").equals(myHostName))){
					System.out.println("Peer server is running on " + peerHostName);
				}
				if(! (aliveNodes.contains(peerHostName)) ){
					aliveNodes.add(peerHostName);
				}
				System.out.println("Current active peers: ");
				for(String name : aliveNodes){
					System.out.print(name+"\t");
				}
				System.out.print("\n");
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
			}

			//receive file sync from peer
			if(clientCommand.equals("-$")){
				System.out.print("\n");
				InetAddress peerAddr = serverPacketIn.getAddress();
				clientHostName = peerAddr.getHostName();
				System.out.println("File sync from peer " + clientHostName);
				serverBufferIn = new byte[2048];

		
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}

				inpuString = new String(serverPacketIn.getData());
				this.setFileName(inpuString);
				System.out.println("File to receive: "+ inpuString);
				

				serverBufferIn = new byte[2048];
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				ByteArrayInputStream byteInStr = new ByteArrayInputStream(serverPacketIn.getData());
				DataInputStream numIn = new DataInputStream(byteInStr);
				int totalPacketNum = numIn.readInt();	
				System.out.println("Total number of packets to receive: "+ totalPacketNum);

				int count = 0;
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				OutputStream receiveFile = new FileOutputStream(this.getHomeFolder()+"/"+this.getFileName());

				while(true && (count<totalPacketNum)){
					count++;
					serverPacketIn = new DatagramPacket(serverBufferIn, serverBufferIn.length);
					try{
						serverSocket.receive(serverPacketIn);
					}catch(IOException ioe){
						System.out.println("Error when receiving: " + ioe);
					}
					byte[] tmp = serverPacketIn.getData();
					if( count == totalPacketNum){
						break;
					}
					byteStream.write(tmp,0,this.getPacketLength());
					
				}
				System.out.println("File received.");
				byteStream.writeTo(receiveFile);
				receiveFile.close();
				File file = new File(this.getHomeFolder() + "/" + this.getFileName());
				long fileModTime = file.lastModified();
				dfsFile newfile = new dfsFile(this.getFileName(), fileModTime);
				myFiles.add(newfile);				

				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				
					
			}
			
			//initialize DFS
			if(clientCommand.equals(commands[0])){
				System.out.print("\n");
				clientAddr = serverPacketIn.getAddress();
				clientHostName = clientAddr.getHostName();
				System.out.println("Receive a signal from client on " + clientHostName);
				System.out.println("Initialize DFS: sending signal to peers.");
				this.pingPeers();


				if(myFiles.size() > 0){

					for(dfsFile file : myFiles){
						System.out.println("File name: " + file.getFileName() +"\tLast modified: " + file.getLastModTime()+ "\tVersion: " + file.getVersionNum());
					}


					long maxTime = 0;
					String fileToSend = "";
					for(dfsFile file : myFiles){
						if(file.getLastModTime() > maxTime ){
							maxTime = file.getLastModTime();
							fileToSend = file.getFileName();
						}
					}

					this.syncPeers();
					this.setFileName(fileToSend);
					myAddr = InetAddress.getLocalHost();

					//send filename and then file to peer
					for(String peer : aliveNodes){

						InetAddress peerAddr = InetAddress.getByName(peer);
						String myIp = myAddr.getHostAddress();
						String peerIp = peerAddr.getHostAddress();

						//System.out.println("Local host: " + myIp + "Peer host: " + peerIp);
						//System.out.println(myIp.compareTo(peerIp));

						if( (myIp.compareTo(peerIp))!= 0){
							System.out.print("\n");

							System.out.println("File sync to peer " + peer);
							byte[] fileName = new byte[2048];
							fileName = fileToSend.getBytes();

							try{
								DatagramPacket serverPacketOut = new DatagramPacket(fileName, fileName.length, peerAddr, myPort );
								serverSocket.send(serverPacketOut);

							}catch(IOException ioe){
								System.out.println("IO Exception: " + ioe);
							}

							serverBufferIn = new byte[2048];							
							
							System.out.println("File to send: "+ fileToSend);
							
							File localFile = new File(this.getHomeFolder() + "/" + this.getFileName());
							int numOfBytes = (int)localFile.length();
							byte[] byteArray = new byte[numOfBytes];
							System.out.println("This file has " + numOfBytes + " of bytes.");

							FileInputStream localFileIn = null;
							int numOfPackets = (int)(numOfBytes / (this.getPacketLength()))+1;
							ByteArrayOutputStream byteOutStr = new ByteArrayOutputStream();
							DataOutputStream intOut = new DataOutputStream(byteOutStr);	
							intOut.writeInt(numOfPackets);
							byte[] packetNum = byteOutStr.toByteArray();
							
							try{
								DatagramPacket serverPacketOut = new DatagramPacket(packetNum, packetNum.length, peerAddr, myPort );
								serverSocket.send(serverPacketOut);

							}catch(IOException ioe){
								System.out.println("IO Exception: " + ioe);
							}
							System.out.println("It will be sent by " + numOfPackets + " number of packets of length " + this.getPacketLength());
							serverBufferIn = new byte[2048];
							
							try{
								localFileIn = new FileInputStream(localFile);
								localFileIn.read(byteArray);

								localFileIn.close();
							}catch(FileNotFoundException fne){
								System.out.println("Error: file is not found.");
							}catch(IOException ioe){
								System.out.println("IO Exception.");
							}
							
								
							this.setInterval(10);
							System.out.println("Start Sending...");
							if(byteArray.length <= this.getPacketLength()){
								try{	
									DatagramPacket serverPacketOut = new DatagramPacket(byteArray, byteArray.length, peerAddr, myPort);
									serverSocket.send(serverPacketOut);
								}catch(IOException ioe){
									System.out.println("IO Exception :" + ioe);
								}
							}else{
								int packetlen = this.getPacketLength();
								byte[] dataPacket = new byte[packetlen];
								for(int i=0; i<numOfPackets; i++){
									if( i == numOfPackets-1){
										System.arraycopy( byteArray, i*packetlen, dataPacket, 0 , (byteArray.length % packetlen));
									}else{
										System.arraycopy( byteArray, i*packetlen, dataPacket, 0 , packetlen );
									}
									try{
										Thread.sleep(interval);
										DatagramPacket serverPacketOut = new DatagramPacket(dataPacket, dataPacket.length, peerAddr, myPort);
										serverSocket.send(serverPacketOut);	
								
									}catch(IOException ioe){
										System.out.println("IO Exception :" + ioe);
									}catch(InterruptedException ie){
										System.out.println("Interrupted Exception :" + ie);
									}
								}
							}
							System.out.println("File sent.");


						//TODO:mark

						}
						
					}
				}
				
				
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
			}

			//list all files in DFS
			if(clientCommand.equals(commands[1])){
				System.out.print("\n");
				clientAddr = serverPacketIn.getAddress();
				clientHostName = clientAddr.getHostName();
				System.out.println("Receive a signal from client on " + clientHostName);
				
				System.out.println("List all files in DFS ");
				File dir = new File( this.getHomeFolder());
				String files[] = dir.list();
				System.out.println("There are " + files.length + " files in DFS.");
				
				int numOfFiles = files.length;
				ByteArrayOutputStream byteOutStr = new ByteArrayOutputStream();
				DataOutputStream intOut = new DataOutputStream(byteOutStr);
				intOut.writeInt(numOfFiles);
				byte[] fileNum = byteOutStr.toByteArray();	
				DatagramPacket serverPacketOut = new DatagramPacket(fileNum, fileNum.length, clientAddr, myPort);
				serverSocket.send(serverPacketOut);
				
				if(files == null){
					System.out.println("No files");
				}else{
					for(int i=0; i<files.length; i++){
						System.out.println(files[i]);
						byte[] tmp = new byte[2048];
						tmp = files[i].getBytes();
						try{
							serverPacketOut = new DatagramPacket(tmp, tmp.length, clientAddr, myPort);
							serverSocket.send(serverPacketOut);
						}catch(IOException ioe){
							System.out.println("IO Exception :" + ioe);
						}
					}
				}
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
			}
		
			//client send file to server (upload)
			if(clientCommand.equals(commands[2])){
				System.out.print("\n");
				clientAddr = serverPacketIn.getAddress();
				clientHostName = clientAddr.getHostName();
				System.out.println("File sending from client " + clientHostName);
				serverBufferIn = new byte[2048];
				//serverSocket.receive(serverPacketIn);
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				inpuString = new String(serverPacketIn.getData());
				this.setFileName(inpuString);
				System.out.println("File to receive: "+ inpuString);
				
				//System.out.println("Homefolder: " + this.getHomeFolder());
				serverBufferIn = new byte[2048];
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				ByteArrayInputStream byteInStr = new ByteArrayInputStream(serverPacketIn.getData());
				DataInputStream numIn = new DataInputStream(byteInStr);
				int totalPacketNum = numIn.readInt();	
				System.out.println("Total number of packets to receive: "+ totalPacketNum);

				int count = 0;
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				OutputStream receiveFile = new FileOutputStream(this.getHomeFolder()+"/"+this.getFileName());

				while(true && (count<totalPacketNum)){
					count++;
					serverPacketIn = new DatagramPacket(serverBufferIn, serverBufferIn.length);
					try{
						serverSocket.receive(serverPacketIn);
					}catch(IOException ioe){
						System.out.println("Error when receiving: " + ioe);
					}
					byte[] tmp = serverPacketIn.getData();
					byteStream.write(tmp,0,this.getPacketLength());
					if( count == totalPacketNum){
						break;
					}
					
				}
				System.out.println("File received.");
				byteStream.writeTo(receiveFile);
				receiveFile.close();
				File file = new File(this.getHomeFolder() + "/" + this.getFileName());
				long fileModTime = file.lastModified();
				dfsFile newfile = new dfsFile(this.getFileName(), fileModTime);
				myFiles.add(newfile);
				

				try{
					serverPacketIn = new DatagramPacket( serverBufferIn, 0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
			}


			//create new file
			if(clientCommand.equals(commands[7])){
				clientAddr = serverPacketIn.getAddress();
				clientHostName = clientAddr.getHostName();
				System.out.println("File created by client " + clientHostName);
				serverBufferIn = new byte[2048];
				//serverSocket.receive(serverPacketIn);
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				inpuString = new String(serverPacketIn.getData());
				this.setFileName(inpuString);
				System.out.println("File name: "+ inpuString);
				
				//System.out.println("Homefolder: " + this.getHomeFolder());
				serverBufferIn = new byte[2048];
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				ByteArrayInputStream byteInStr = new ByteArrayInputStream(serverPacketIn.getData());
				DataInputStream numIn = new DataInputStream(byteInStr);
				int totalPacketNum = numIn.readInt();	
				//System.out.println("Total number of packets to receive: "+ totalPacketNum);

				int count = 0;
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				OutputStream receiveFile = new FileOutputStream(this.getHomeFolder()+"/"+this.getFileName());

				while(true && (count<totalPacketNum)){
					count++;
					serverPacketIn = new DatagramPacket(serverBufferIn, serverBufferIn.length);
					try{
						serverSocket.receive(serverPacketIn);
					}catch(IOException ioe){
						//System.out.println("Error when receiving: " + ioe);
					}
					byte[] tmp = serverPacketIn.getData();
					byteStream.write(tmp,0,this.getPacketLength());
					if( count == totalPacketNum){
						break;
					}
					
				}
				System.out.println("File created.");
				byteStream.writeTo(receiveFile);
				receiveFile.close();
				File file = new File(this.getHomeFolder() + "/" + this.getFileName());
				long fileModTime = file.lastModified();
				dfsFile newfile = new dfsFile(this.getFileName(), fileModTime);
				myFiles.add(newfile);
				

				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
			}


		
			//client receive file from server (download)
			if(clientCommand.equals(commands[3])){
				System.out.print("\n");
				clientAddr = serverPacketIn.getAddress();
				clientHostName = clientAddr.getHostName();
				System.out.println("File download request from client " + clientHostName);

				
				serverBufferIn = new byte[2048];
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				inpuString = new String(serverPacketIn.getData());
				this.setFileName(inpuString);
						
				File localFile = new File(this.getHomeFolder() + "/" + this.getFileName());

				while(!localFile.exists()){
					System.out.println("Error: no such file, correct request after timeout.");
					serverBufferIn = new byte[2048];
					try{
						serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
						serverSocket.receive(serverPacketIn);

					}catch(IOException ioe){
						System.out.println("IO Exception: " + ioe);
					}
				inpuString = new String(serverPacketIn.getData());
				this.setFileName(inpuString);						
				localFile = new File(this.getHomeFolder() + "/" + this.getFileName());
				}

				int numOfBytes = (int)localFile.length();
				byte[] byteArray = new byte[numOfBytes];
				System.out.println("File to send: "+ inpuString);		
				System.out.println("This file has " + numOfBytes + " of bytes.");

				FileInputStream localFileIn = null;
				int numOfPackets = (int)(numOfBytes / (this.getPacketLength()))+1;
				ByteArrayOutputStream byteOutStr = new ByteArrayOutputStream();
				DataOutputStream intOut = new DataOutputStream(byteOutStr);	
				intOut.writeInt(numOfPackets);
				byte[] packetNum = byteOutStr.toByteArray();
				
				try{
					DatagramPacket serverPacketOut = new DatagramPacket(packetNum, packetNum.length, clientAddr, myPort );
					serverSocket.send(serverPacketOut);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				System.out.println("It will be sent by " + numOfPackets + " number of packets of length " + this.getPacketLength());
				
				try{
					localFileIn = new FileInputStream(localFile);
					localFileIn.read(byteArray);

					localFileIn.close();
				}catch(FileNotFoundException fne){
					System.out.println("Error: file is not found.");
				}catch(IOException ioe){
					System.out.println("IO Exception.");
				}
				
			 		
				this.setInterval(10);
				System.out.println("Start Sending...");
				if(byteArray.length <= this.getPacketLength()){
					try{	
						DatagramPacket serverPacketOut = new DatagramPacket(byteArray, byteArray.length, clientAddr, myPort);
						serverSocket.send(serverPacketOut);
					}catch(IOException ioe){
						System.out.println("IO Exception :" + ioe);
					}
				}else{
					int packetlen = this.getPacketLength();
					byte[] dataPacket = new byte[packetlen];
					for(int i=0; i<numOfPackets; i++){
						if( i == numOfPackets-1){
							System.arraycopy( byteArray, i*packetlen, dataPacket, 0 , (byteArray.length % packetlen));
						}else{
							System.arraycopy( byteArray, i*packetlen, dataPacket, 0 , packetlen );
						}
						try{
							Thread.sleep(interval);
							DatagramPacket serverPacketOut = new DatagramPacket(dataPacket, dataPacket.length, clientAddr, myPort);
							serverSocket.send(serverPacketOut);	
					
						}catch(IOException ioe){
							System.out.println("IO Exception :" + ioe);
						}catch(InterruptedException ie){
							System.out.println("Interrupted Exception :" + ie);
						}
					}
				}
				System.out.println("File sent.");

				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
		
	
			}

			//delete file
			if(clientCommand.equals(commands[4])){
				System.out.print("\n");
				clientAddr = serverPacketIn.getAddress();
				clientHostName = clientAddr.getHostName();
				System.out.println("File delete request from client " + clientHostName);
				serverBufferIn = new byte[2048];
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				inpuString = new String(serverPacketIn.getData());
				this.setFileName(inpuString);
				System.out.println("File to delete: "+ inpuString);
				File tmp = new File(this.getHomeFolder() + "/" + this.getFileName());
				if(tmp.exists()){
	       		 		try{           
			    			Runtime rt = Runtime.getRuntime();
						Process del = rt.exec("rm -rf "+ this.getHomeFolder() + "/" + this.getFileName());
						System.out.println("File " + this.getFileName() + " is deleted.");
	
      			  		}catch(Exception e){
                				System.out.println("Error:" + e.getMessage());
	  	  			}	
				}
				
					
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
			}



			//edit file
			if(clientCommand.equals(commands[5])){
				System.out.print("\n");
				clientAddr = serverPacketIn.getAddress();
				clientHostName = clientAddr.getHostName();
				System.out.println("File edit request from client " + clientHostName);
				serverBufferIn = new byte[2048];
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
				inpuString = new String(serverPacketIn.getData());
				this.setFileName(inpuString);

				File localFile = new File(this.getHomeFolder() + "/" + this.getFileName());
				while(!localFile.exists()){
					System.out.println("Error: no such file, correct request after timeout.");
					serverBufferIn = new byte[2048];
					try{
						serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
						serverSocket.receive(serverPacketIn);

					}catch(IOException ioe){
						System.out.println("IO Exception: " + ioe);
					}
				inpuString = new String(serverPacketIn.getData());
				this.setFileName(inpuString);						
				localFile = new File(this.getHomeFolder() + "/" + this.getFileName());
				}


				System.out.println("File to edit: "+ inpuString);

				//increase version number
				for(dfsFile file : myFiles){
					if(file.getFileName().equals(inpuString)){
						int tmpVer = file.getVersionNum();
						file.setVersionNum(tmpVer+1);
					}
				}
				File tmp = new File(this.getHomeFolder() + "/" + this.getFileName());
				FileLock fileLock = null;

				if(tmp.exists()){

						try
						{
							RandomAccessFile file = new RandomAccessFile(this.getHomeFolder() + "/" + this.getFileName(), "rw");
							FileChannel fileChannel = file.getChannel();				 
							fileLock = fileChannel.tryLock();

							if (fileLock != null){
								System.out.println("File " + this.getFileName() + " is locked");
								try{           
			    						Runtime rt = Runtime.getRuntime();
									Process edit = rt.exec("/usr/bin/xterm  vi "+ this.getHomeFolder() + "/" + this.getFileName());
									edit.waitFor();
									Thread.sleep(30000);
									System.out.println("File " + this.getFileName() + " is edited.");
	
      			  				}catch(Exception e){
                					System.out.println("Error:" + e.getMessage());
	  	  						}

							}
						}finally{
							if (fileLock != null){
								fileLock.release();
								System.out.println("Lock on " + this.getFileName() + " is released");
							}
						}


	       		 		
				}
				
					
				try{
					serverPacketIn = new DatagramPacket( serverBufferIn,0, serverBufferIn.length);
					serverSocket.receive(serverPacketIn);
					inpuString = new String(serverPacketIn.getData());
					clientCommand = inpuString.substring(0,2);

				}catch(IOException ioe){
					System.out.println("IO Exception: " + ioe);
				}
			}





			
		   	if(clientCommand.equals(commands[6])){	
				break;
			}
		}
		
			serverSocket.close();
			System.out.println("Server quit.");

		}catch(UnknownHostException ue){
			System.out.println("Unknown host: " + ue);
		}catch(IOException ioe){
			System.out.println("IO Exception: " + ioe);
		}
	}

	public void pingPeers(){
		String serverSig = "-#";
		byte[] peerSig = new byte[2];
		peerSig = serverSig.getBytes();
		for(int i=0; i<cluster.length; i++){
			try {
				InetAddress nodeAddr = InetAddress.getByName(cluster[i]);
				DatagramPacket serverSigOut = new DatagramPacket(peerSig, peerSig.length, nodeAddr, myPort);
				serverSocket.send(serverSigOut);
			}catch(IOException ioe) {
				System.out.println("IO Exception: " + ioe);
			}
		}
	}
	
	public void syncPeers(){
		String serverSig = "-$";
		byte[] peerSig = new byte[2];
		peerSig = serverSig.getBytes();
		for(int i=0; i<cluster.length; i++){
			try {
				InetAddress nodeAddr = InetAddress.getByName(cluster[i]);
				InetAddress myAddr = InetAddress.getLocalHost();
				if(nodeAddr.equals(myAddr)){
					continue;
				}
				DatagramPacket serverSigOut = new DatagramPacket(peerSig, peerSig.length, nodeAddr, myPort);
				serverSocket.send(serverSigOut);
			}catch(IOException ioe) {
				System.out.println("IO Exception: " + ioe);
			}
		}
	}
	
	public void createHomeFolder(String homeDir) {
		File dir = new File("/tmp/"+ homeDir);
        	if(dir.exists()){
			System.out.println("Error: directory exists.");
                	System.exit(1);
	    	}else{    
       		 	try{           
	    			Runtime rt = Runtime.getRuntime();
				//Process mkdir = rt.exec("act_exec -a mkdir /tmp/"+homeDir);
				Process mkdir = rt.exec("mkdir /tmp/"+homeDir);
				System.out.println("Directory:" + homeDir + " is created.");

        		}catch(Exception e){
                		System.out.println("Error:" + e.getMessage());
	    		}	
		}
	}
	
	public boolean ping ( String hostName ){
		boolean result = false;
		//Runtime rt = Runtime.getRuntime();
		try{
			Runtime rt = Runtime.getRuntime();
	  		Process knock = rt.exec("ping -c 1 "+hostName);
			int returnVal = knock.waitFor();
			result = (returnVal==0);
		}catch(IOException ioe){
			System.out.println("IO exception: " +ioe);
		}
		//catch(UnknownHostException uhe){
		//	System.out.println("Unknown host: " + uhe);
		//}
		catch(InterruptedException ie){
			System.out.println("Interrupted: " + ie);
		}
		return result;
	}
	

	public void collectAliveServers() {
		for(int i=0; i<cluster.length; i++){
			try {
				if( ping ( cluster[i] ) ){
					System.out.println("Host " + cluster[i] + " is alive.");
				}else{
					System.out.println("Host " + cluster[i] + " is not responding.");
				}
			} catch (Exception e) {
				System.out.println("Exception: " + e);
			}
		}
		
	}






	public static String getHomeFolder() {
		return homeFolder;
	}


	public static void setHomeFolder(String homeFolder) {
		ServerImpl.homeFolder = homeFolder;
	}


	public static int getServerPort() {
		return serverPort;
	}


	public static void setServerPort(int serverPort) {
		ServerImpl.serverPort = serverPort;
	}


	public static List<String> getAliveNodes() {
		return aliveNodes;
	}


	public static void setAliveNodes(List<String> aliveNodes) {
		ServerImpl.aliveNodes = aliveNodes;
	}


	public static List<dfsFile> getMyFiles() {
		return myFiles;
	}


	public static void setMyFiles(List<dfsFile> myFiles) {
		ServerImpl.myFiles = myFiles;
	}


	public static String getFileName() {
		return fileName;
	}


	public static void setFileName(String fileName) {
		ServerImpl.fileName = fileName;
	}

	public static void setInterval(int interval){
		ServerImpl.interval = interval;
	}
	
	public static int getInterval(){
		return interval;
	}

	public static void setPacketLength(int packetLength){
		ServerImpl.packetLength = packetLength;
	}
	
	public static int getPacketLength(){
		return packetLength;
	}

}
