package bittorrent;

public class Peer {
	private String id;
	private String ip;
	private int port; 
	
	public Peer(String id, String ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
	}
	
	public String getId() {
		return id;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
}
