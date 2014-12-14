
import java.net.UnknownHostException;
import java.util.List;

public interface Server {
	//Pool of all working servers
	public void collectAliveServers();
	
	//Initial create home folder
	public void createHomeFolder(String homeDir);
	
	//true if host responses
	public boolean ping(String hostName);

	//start the udp socket on server side
	public void startUdpSocket();	

}
