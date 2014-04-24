package bittorrent;

import java.io.UnsupportedEncodingException;

public class Peer {
	private byte[] id;
	private String ip;
	private int port; 
	
	public Peer(byte[] id, String ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
	}
	
	public byte[] getId() {
		return id;
	}
	
	public String getIdAsString(){
	  try {
	  return new String(this.id,"US-ASCII");
	  }catch(UnsupportedEncodingException uee){
	    return Tracker.toHex(this.id);
	  }
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
	
	public String toString(){
	  StringBuilder sb = new StringBuilder();
	    sb.append(this.getIdAsString()).append(" (").append(this.ip).append(':').append(this.port).append(")");
	  return sb.toString();
	}
}
